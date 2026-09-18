<#
.SYNOPSIS
    Plays real Force Item Battle rounds against a live server and asserts what reached FIBService.

.DESCRIPTION
    The unit suite in src/test covers rules reachable without a server. This covers the half that
    cannot be: whether a round actually starts, whether a player is outfitted correctly, whether
    obtaining an item registers as a find, and whether the right rows land in FIBService. Every one
    of those crosses a process boundary, so nothing short of playing a round proves it.

    Bots join through understudy-client (a headless Java-Edition client speaking the wire protocol),
    the server is driven over RCON, and assertions read the bots' inventories and the service's REST
    API — never chat, which the client cannot see.

    -Mode solo  two bots, one round. Covers round setup, finds, and the solo stat rows.
    -Mode team  four bots (the minimum /start accepts for teams). Covers the joker split across
                members and, most importantly, that the shared team row is written ONCE per round
                rather than once per member.

    Both modes play TWO rounds in one server session on purpose. The second is a regression test:
    RecipeManager.initRecipes used to throw "Duplicate recipe ignored" and silently abort the second
    startGame of a session, which production never saw because scheduleReset restarts the JVM.

.PREREQUISITES
    - understudy-client on PATH or via -ClientPath:
        go install github.com/blocktopiaworld/understudy-client/cmd/understudy-client@latest
    - FIBService running, with MariaDB behind it. Default http://127.0.0.7:29708.
    - run/server.properties needs offline-mode and RCON; pass -ConfigureServer to set them.

.EXAMPLE
    .\scripts\Invoke-RoundTest.ps1 -Mode solo -ConfigureServer

.EXAMPLE
    .\scripts\Invoke-RoundTest.ps1 -Mode team
#>
[CmdletBinding()]
param(
    [ValidateSet("solo", "team")][string]$Mode = "solo",
    [string]$ClientPath = "understudy-client",
    [string]$ServiceUrl = "http://127.0.0.7:29708",
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 25565,
    [int]$RconPort = 25575,
    [string]$RconPassword = "understudy-local",
    # The sweep walks ~1,380 items in batches and takes over a minute; the round has to outlast it.
    [int]$RoundMinutes = 3,
    [int]$Jokers = 3,
    [switch]$ConfigureServer,
    [switch]$KeepRunning
)

$ErrorActionPreference = "Stop"
$pluginRoot = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot "Rcon.ps1")

$workDir = Join-Path ([System.IO.Path]::GetTempPath()) ("fib-roundtest-$Mode-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Force $workDir | Out-Null
$serverLog = Join-Path $workDir "server.log"

# Four is not a preference: CommandStart disables TEAM below four players and clears the teams.
$botCount = if ($Mode -eq "team") { 4 } else { 2 }
$bots = 0..($botCount - 1) | ForEach-Object {
    [pscustomobject]@{ Name = "Understudy$($_ + 1)"; Port = 8080 + $_; Uuid = $null }
}

# --------------------------------------------------------------------------- assertions

$script:Results = @()

function Assert-That([string]$Name, [bool]$Condition, [string]$Detail = "") {
    $script:Results += [pscustomobject]@{ Name = $Name; Passed = $Condition; Detail = $Detail }
    if ($Condition) { Write-Host ("  PASS  " + $Name) -ForegroundColor Green }
    else { Write-Host ("  FAIL  " + $Name + $(if ($Detail) { " -- $Detail" })) -ForegroundColor Red }
}

function Wait-Until([scriptblock]$Condition, [int]$TimeoutSeconds, [int]$PollSeconds = 2) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try { if (& $Condition) { return $true } } catch { }
        Start-Sleep -Seconds $PollSeconds
    }
    return $false
}

# --------------------------------------------------------------------------- helpers

function Get-BotState($bot) { Invoke-RestMethod "http://127.0.0.1:$($bot.Port)/state" -TimeoutSec 10 }
function Get-BotInventory($bot) { Invoke-RestMethod "http://127.0.0.1:$($bot.Port)/inventory" -TimeoutSec 10 }
function Get-SoloStats($bot) { Invoke-RestMethod "$ServiceUrl/statistics/solo/$($bot.Uuid)" -TimeoutSec 20 }

# A team row that has never been written 404s; that is a zeroed row, not a failure.
function Get-TeamStats([string]$a, [string]$b) {
    try { return Invoke-RestMethod "$ServiceUrl/statistics/team/$a/$b" -TimeoutSec 20 }
    catch { return $null }
}

function Get-TeamGamesPlayed([string]$a, [string]$b) {
    $row = Get-TeamStats $a $b
    if ($null -eq $row) { return 0 }
    return [int]$row.gamesPlayed
}

function Get-Teammates([string]$uuid) {
    try { return Invoke-RestMethod "$ServiceUrl/statistics/team/$uuid/teammates" -TimeoutSec 20 }
    catch { return @() }
}

function Get-JokerCount($bot) {
    $inv = Get-BotInventory $bot
    $slot = $inv.items | Where-Object { [int]$_.slot -eq 40 -and $_.name -eq "minecraft:barrier" }
    if ($slot) { return [int]$slot.count }
    return 0
}

function Stop-Everything {
    Get-Process -Name ([System.IO.Path]::GetFileNameWithoutExtension($ClientPath)) -EA SilentlyContinue |
        Stop-Process -Force -EA SilentlyContinue
    foreach ($p in @($ServerPort, $RconPort)) {
        foreach ($c in (Get-NetTCPConnection -LocalPort $p -State Listen -EA SilentlyContinue)) {
            Stop-Process -Id $c.OwningProcess -Force -EA SilentlyContinue
        }
    }
}

# TEAM has no console command — the settings GUI is player-only — so it is set in the plugin config
# before boot. Written while the server is down: the plugin rewrites this file on disable.
# CommandStart also writes isTeamGame=false itself when fewer than four players are present, so this
# has to be set on every run rather than once.
function Set-TeamSetting([bool]$Enabled) {
    $cfg = Join-Path $pluginRoot "run\plugins\ForceItemBattle\config.yml"
    if (-not (Test-Path $cfg)) { return $false }

    $value = if ($Enabled) { "true" } else { "false" }
    $text = [System.IO.File]::ReadAllText($cfg, [System.Text.Encoding]::UTF8)

    # GameSetting prefixes every path with "settings.", so the live key is settings.isTeamGame —
    # two spaces of indent. The identical key also appears at six spaces under
    # presets.<name>.settings, which is a different setting for a different game; matching on the
    # exact indent is what keeps this off it.
    $updated = [regex]::Replace($text, '(?m)^  isTeamGame:[^\r\n]*$', "  isTeamGame: $value", 1)
    if ($updated -eq $text) { return $false }

    [System.IO.File]::WriteAllText($cfg, $updated, (New-Object System.Text.UTF8Encoding($false)))
    return $true
}

# Reads settings.isTeamGame back, so a silently-failed edit shows up as a failed assertion rather
# than as a confusing SOLO match at the end of a team run.
function Get-TeamSetting {
    $cfg = Join-Path $pluginRoot "run\plugins\ForceItemBattle\config.yml"
    if (-not (Test-Path $cfg)) { return $null }
    $text = [System.IO.File]::ReadAllText($cfg, [System.Text.Encoding]::UTF8)
    $m = [regex]::Match($text, '(?m)^  isTeamGame:\s*(\S+)\s*$')
    if (-not $m.Success) { return $null }
    return ($m.Groups[1].Value -eq "true")
}

# --------------------------------------------------------------------------- item pool
#
# Parsed from the plugin source rather than hardcoded, so it cannot drift from the real pool.
#
# Every registered material, not just EARLY. Two reasons, both learned the hard way: MID unlocks
# around 11% into a round and LATE around 29%, so by the time the sweep runs the target may be from
# either; and with HARD/EXTREME/END all on — the default — nothing is filtered out, so a nether or
# end item is a perfectly ordinary target. Sweeping only EARLY made the find assertion a coin flip.

function Get-PoolItems {
    $source = Join-Path $pluginRoot "src\main\java\forceitembattle\manager\ItemDifficultiesManager.java"
    $text = [System.IO.File]::ReadAllText($source, [System.Text.Encoding]::UTF8)
    $pattern = [regex]'register\(Material\.(\w+),\s*State\.\w+'
    return $pattern.Matches($text) | ForEach-Object { $_.Groups[1].Value.ToLower() }
}

# Drops every pool item at each bot's feet, clearing between batches.
#
# A dropped item fires EntityPickupItemEvent, one of the triggers ItemsListener watches. /give does
# not fire it, and a full inventory stops pickups altogether — hence the batching and the clear,
# which is what makes this reliable rather than a coin flip.
function Invoke-PoolSweep($conn, [string[]]$items, [int]$BatchSize = 20) {
    Invoke-Rcon $conn "clear @a" | Out-Null
    for ($i = 0; $i -lt $items.Count; $i += $BatchSize) {
        $slice = $items[$i..([Math]::Min($i + $BatchSize - 1, $items.Count - 1))]
        foreach ($item in $slice) {
            foreach ($bot in $bots) {
                Invoke-Rcon $conn "execute at $($bot.Name) run summon item ~ ~0.5 ~ {Item:{id:`"minecraft:$item`",count:1}}" | Out-Null
            }
        }
        Start-Sleep -Milliseconds 900
        Invoke-Rcon $conn "clear @a" | Out-Null
    }
}

# --------------------------------------------------------------------------- run

try {
    Write-Host "`n== preflight ($Mode) ==" -ForegroundColor Cyan

    $clientResolved = (Get-Command $ClientPath -EA SilentlyContinue)
    if (-not $clientResolved) { throw "understudy-client not found at '$ClientPath'. See .PREREQUISITES." }
    $ClientPath = $clientResolved.Source

    $health = Invoke-RestMethod "$ServiceUrl/actuator/health" -TimeoutSec 15
    Assert-That "FIBService is up" ($health.status -eq "UP") "status=$($health.status)"

    # understudy-client writes its banner to stderr. In Windows PowerShell each stderr line from a
    # native command becomes an ErrorRecord, which $ErrorActionPreference="Stop" turns into a thrown
    # exception on an otherwise clean exit — so the preference is relaxed for this call only.
    $versions = & { $ErrorActionPreference = "Continue"; (& $ClientPath -versions 2>&1 | Out-String) }
    Assert-That "client speaks this server's protocol" ($versions -match "26\.2|26\.1") $versions.Trim()

    if ($ConfigureServer) {
        $props = Join-Path $pluginRoot "run\server.properties"
        if (Test-Path $props) {
            $text = Get-Content $props -Raw
            $text = $text -replace '(?m)^online-mode=.*$', 'online-mode=false'
            $text = $text -replace '(?m)^enforce-secure-profile=.*$', 'enforce-secure-profile=false'
            $text = $text -replace '(?m)^enable-rcon=.*$', 'enable-rcon=true'
            $text = $text -replace '(?m)^rcon\.password=.*$', "rcon.password=$RconPassword"
            [System.IO.File]::WriteAllText($props, $text, (New-Object System.Text.UTF8Encoding($false)))
            Write-Host "  configured run/server.properties for offline-mode + RCON"
        }
    }

    Write-Host "`n== starting server ==" -ForegroundColor Cyan
    Stop-Everything
    Start-Sleep -Seconds 3

    $teamWanted = ($Mode -eq "team")
    Set-TeamSetting $teamWanted | Out-Null
    $actualSetting = Get-TeamSetting
    Assert-That "settings.isTeamGame is $teamWanted before boot" ($actualSetting -eq $teamWanted) "config reads '$actualSetting'"

    Push-Location $pluginRoot
    Start-Process -FilePath (Join-Path $pluginRoot "gradlew.bat") `
        -ArgumentList "runServer", "--console=plain" `
        -RedirectStandardOutput $serverLog `
        -RedirectStandardError (Join-Path $workDir "server.err.log") `
        -WindowStyle Hidden
    Pop-Location
    $up = Wait-Until { (Get-Content $serverLog -Raw -EA SilentlyContinue) -match 'Done \([\d.]+s\)!' } 420 3
    Assert-That "server reached 'Done'" $up "log: $serverLog"
    if (-not $up) { throw "server never started" }

    Write-Host "`n== joining $botCount bots ==" -ForegroundColor Cyan
    foreach ($bot in $bots) {
        Start-Process -FilePath $ClientPath `
            -ArgumentList "-addr", "${ServerHost}:$ServerPort", "-username", $bot.Name,
                          "-control", "127.0.0.1:$($bot.Port)", "-hold", "0" `
            -RedirectStandardOutput (Join-Path $workDir "$($bot.Name).log") `
            -RedirectStandardError (Join-Path $workDir "$($bot.Name).err.log") `
            -WindowStyle Hidden
    }
    $joined = Wait-Until {
        (@($bots | Where-Object { -not (Get-BotState $_).joined }).Count -eq 0)
    } 90 2
    Assert-That "all $botCount bots joined" $joined
    if (-not $joined) { throw "bots never joined" }
    foreach ($bot in $bots) { $bot.Uuid = (Get-BotState $bot).uuid }

    $conn = Connect-Rcon -RconHost $ServerHost -Port $RconPort -Password $RconPassword
    foreach ($bot in $bots) { Invoke-Rcon $conn "op $($bot.Name)" | Out-Null }

    $soloBaseline = @{}
    foreach ($bot in $bots) { $soloBaseline[$bot.Name] = Get-SoloStats $bot }

    # Every unordered pair, so whichever two the pairing picks has a baseline to compare against.
    # Teams are formed fresh each round and deliberately avoid repeating previous pairings, so the
    # pair cannot be known in advance.
    $pairBaseline = @{}
    if ($teamWanted) {
        for ($i = 0; $i -lt $bots.Count; $i++) {
            for ($j = $i + 1; $j -lt $bots.Count; $j++) {
                $key = "$($bots[$i].Uuid)|$($bots[$j].Uuid)"
                $pairBaseline[$key] = Get-TeamGamesPlayed $bots[$i].Uuid $bots[$j].Uuid
            }
        }
    }

    # ---------------------------------------------------------------- round one
    Write-Host "`n== round 1 ==" -ForegroundColor Cyan
    $startReply = Invoke-Rcon $conn "start $RoundMinutes $Jokers"
    if ($teamWanted) {
        Assert-That "teams were not disabled for lack of players" (-not ($startReply -match "not enough players")) $startReply.Trim()
    }

    $started = Wait-Until { (Get-BotState $bots[0]).game_mode -eq "survival" } 45 2
    Assert-That "round started (players in survival)" $started

    if ($started) {
        # Round setup, read off the wire. Bukkit hotbar index N is protocol slot 36+N, so the joker
        # slot 4 reads as slot 40.
        $inv = Get-BotInventory $bots[0]
        $bySlot = @{}
        foreach ($it in $inv.items) { $bySlot[[int]$it.slot] = $it }

        Assert-That "starting kit: stone axe in slot 36"     ($bySlot[36].name -eq "minecraft:stone_axe")     $bySlot[36].name
        Assert-That "starting kit: stone pickaxe in slot 37" ($bySlot[37].name -eq "minecraft:stone_pickaxe") $bySlot[37].name
        Assert-That "starting kit: stone shovel in slot 38"  ($bySlot[38].name -eq "minecraft:stone_shovel")  $bySlot[38].name
        Assert-That "backpack bundle present"                ($bySlot[44].name -match "bundle")               $bySlot[44].name

        $jokerCounts = $bots | ForEach-Object { Get-JokerCount $_ }
        $jokerTotal = ($jokerCounts | Measure-Object -Sum).Sum

        if ($teamWanted) {
            # RoundSetup.splitJokers: each team gets the round's pool split across its members, and
            # every joker in the pool is handed out. Two teams of two, so 3 jokers each becomes
            # 2 + 1 per team and 6 across the server.
            $teamsExpected = $botCount / 2
            Assert-That "joker pool split across team members" ($jokerTotal -eq ($Jokers * $teamsExpected)) "counts=$($jokerCounts -join ',') total=$jokerTotal expected=$($Jokers * $teamsExpected)"
            Assert-That "no member holds the whole team pool" (@($jokerCounts | Where-Object { $_ -eq $Jokers }).Count -eq 0) "counts=$($jokerCounts -join ',')"
            Assert-That "every member got at least one joker" (@($jokerCounts | Where-Object { $_ -lt 1 }).Count -eq 0) "counts=$($jokerCounts -join ',')"
        }
        else {
            Assert-That "joker stack of $Jokers in slot 40" ($bySlot[40].name -eq "minecraft:barrier" -and [int]$bySlot[40].count -eq $Jokers) "$($bySlot[40].name) x$($bySlot[40].count)"
        }

        $pool = Get-PoolItems
        Assert-That "parsed the item pool from source" ($pool.Count -gt 1000) "$($pool.Count) items"
        Write-Host "  sweeping $($pool.Count) items to force finds..."
        Invoke-PoolSweep $conn $pool

        Write-Host "  waiting for the round to end..."
        $ended = Wait-Until { (Get-BotState $bots[0]).game_mode -eq "creative" } ($RoundMinutes * 60 + 150) 5
        Assert-That "round ended (players in creative)" $ended

        if ($ended) {
            Start-Sleep -Seconds 6   # the match PUT and the stat writes are async

            $match = (Invoke-RestMethod "$ServiceUrl/matches?page=0&size=1" -TimeoutSec 20).matches[0]
            $finds = [int]$match.itemsFound
            Assert-That "at least one find was recorded" ($finds -gt 0) "$finds finds across $botCount bots"

            # In a team game a find belongs to the team, so it lands on the shared row and the
            # member row and NOT on the finder's solo row. That routing is the point of
            # PlayerStatsWrite, so it is asserted rather than assumed.
            if ($teamWanted) {
                $soloTouched = 0
                foreach ($bot in $bots) {
                    $after = Get-SoloStats $bot
                    $soloTouched += ([int]$after.totalItemsFound - [int]$soloBaseline[$bot.Name].totalItemsFound)
                }
                Assert-That "team finds did not leak onto solo rows" ($soloTouched -eq 0) "solo delta=$soloTouched"
            }

            if (-not $teamWanted) {
                $soloFinds = 0
                foreach ($bot in $bots) {
                    $before = $soloBaseline[$bot.Name]
                    $after = Get-SoloStats $bot
                    Assert-That "$($bot.Name): gamesPlayed incremented" ([int]$after.gamesPlayed -eq [int]$before.gamesPlayed + 1) "$($before.gamesPlayed) -> $($after.gamesPlayed)"
                    $soloFinds += ([int]$after.totalItemsFound - [int]$before.totalItemsFound)
                }
                Assert-That "solo rows agree with the match on finds" ($soloFinds -eq $finds) "solo=$soloFinds match=$finds"
                $scorer = $bots | Where-Object { ([int](Get-SoloStats $_).totalItemsFound) -gt ([int]$soloBaseline[$_.Name].totalItemsFound) } | Select-Object -First 1
                if ($scorer) {
                    $after = Get-SoloStats $scorer
                    $gained = [int]$after.totalItemsFound - [int]$soloBaseline[$scorer.Name].totalItemsFound
                    Assert-That "$($scorer.Name): highestScore reflects the finds" ([int]$after.highestScore -ge $gained) "score=$($after.highestScore) finds=$gained"
                    Assert-That "$($scorer.Name): time-on-items was measured" ([long]$after.totalTimeSpentOnItems -gt 0) "$($after.totalTimeSpentOnItems)ms"
                    Assert-That "$($scorer.Name): per-item tally populated" ($after.topThreeItems.Count -gt 0) (($after.topThreeItems | ForEach-Object { $_.itemName }) -join ",")
                }
            }
            else {
                # The pairs come from the match's own participants: teamIndex is how the submission
                # records who played together, so grouping by it needs no second source of truth.
                $pairs = $match.participants |
                    Where-Object { $null -ne $_.teamIndex } |
                    Group-Object -Property teamIndex |
                    Where-Object { $_.Count -eq 2 }

                Assert-That "both teams were formed" ($pairs.Count -eq ($botCount / 2)) "$($pairs.Count) pair(s)"

                foreach ($pair in $pairs) {
                    $a = $pair.Group[0].player.uuid
                    $b = $pair.Group[1].player.uuid

                    $key = "$a|$b"
                    if (-not $pairBaseline.ContainsKey($key)) { $key = "$b|$a" }
                    $before = if ($pairBaseline.ContainsKey($key)) { $pairBaseline[$key] } else { 0 }
                    $after = Get-TeamGamesPlayed $a $b

                    # The whole point of the primary-writer rule: both members write the same
                    # normalised row, so a counting stat sent from both sides doubles. It has.
                    Assert-That "team $($pair.Name): round counted once, not twice" (($after - $before) -eq 1) "gamesPlayed $before -> $after"

                    $row = Get-TeamStats $a $b
                    Assert-That "team $($pair.Name): shared row exists" ($null -ne $row)

                    $teamFinds = ([int]$pair.Group[0].itemsFound + [int]$pair.Group[1].itemsFound)
                    if ($null -ne $row -and $teamFinds -gt 0) {
                        Assert-That "team $($pair.Name): shared highestScore reflects the finds" ([int]$row.highestScore -ge $teamFinds) "highestScore=$($row.highestScore) finds=$teamFinds"
                    }

                    # Each member keeps their own contribution row inside the team.
                    foreach ($member in @($a, $b)) {
                        $memberRow = $null
                        try { $memberRow = Invoke-RestMethod "$ServiceUrl/statistics/team/$a/$b/member/$member" -TimeoutSec 20 } catch { }
                        Assert-That "team $($pair.Name): member row written for $($member.Substring(0,8))" ($null -ne $memberRow)
                    }
                }
            }

            Assert-That "match row submitted" ($null -ne $match.matchId) $match.matchId
            Assert-That "match records every participant" ($match.participants.Count -eq $botCount) "$($match.participants.Count) of $botCount"
            Assert-That "match mode is $($Mode.ToUpper())" ($match.mode -eq $Mode.ToUpper()) "mode=$($match.mode)"
            if ($teamWanted) {
                Assert-That "match records the teams" ($match.teams.Count -eq ($botCount / 2)) "$($match.teams.Count) team(s)"
            }
        }
    }

    # ---------------------------------------------------------------- round two
    # Regression: RecipeManager.initRecipes used to throw on the second start of a session,
    # aborting startGame before any player setup happened.
    Write-Host "`n== round 2 (same server session) ==" -ForegroundColor Cyan
    Invoke-Rcon $conn "start 1 $Jokers" | Out-Null
    $restarted = Wait-Until { (Get-BotState $bots[0]).game_mode -eq "survival" } 45 2
    Assert-That "a second round starts in the same session" $restarted "regression: duplicate recipe registration"

    $log = Get-Content $serverLog -Raw -EA SilentlyContinue
    Assert-That "no duplicate-recipe error in the log" (-not ($log -match "Duplicate recipe ignored"))
    Assert-That "no plugin exceptions in the log" (-not ($log -match "\[ForceItemBattle\].*generated an exception"))

    Close-Rcon $conn
}
catch {
    Write-Host "`nERROR: $($_.Exception.Message)" -ForegroundColor Red
    $script:Results += [pscustomobject]@{ Name = "harness completed"; Passed = $false; Detail = $_.Exception.Message }
}
finally {
    if (-not $KeepRunning) { Stop-Everything }

    $passed = @($script:Results | Where-Object { $_.Passed }).Count
    $failed = @($script:Results | Where-Object { -not $_.Passed }).Count
    Write-Host "`n=============================================="
    Write-Host (" $Mode : {0} passed, {1} failed" -f $passed, $failed) -ForegroundColor $(if ($failed) { "Red" } else { "Green" })
    Write-Host " logs: $workDir"
    Write-Host "=============================================="
    if ($failed -gt 0) { exit 1 }
}
