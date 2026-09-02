# Minimal Source-RCON client — enough to drive a local Minecraft server from a test script.
#
# Packet layout: int32 length (excluding itself) | int32 requestId | int32 type | body NUL | NUL
# Types: 3 = auth, 2 = command (and auth response), 0 = response value.
#
# Dot-source this; it exports Connect-Rcon / Invoke-Rcon / Close-Rcon.

function New-RconPacket([int]$id, [int]$type, [string]$body) {
    $bodyBytes = [System.Text.Encoding]::ASCII.GetBytes($body)
    $len = 4 + 4 + $bodyBytes.Length + 2
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([int]$len); $bw.Write([int]$id); $bw.Write([int]$type)
    $bw.Write($bodyBytes); $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Flush()
    return $ms.ToArray()
}

function Read-Exact($stream, [int]$count) {
    $buf = New-Object byte[] $count
    $read = 0
    while ($read -lt $count) {
        $n = $stream.Read($buf, $read, $count - $read)
        if ($n -le 0) { throw "RCON connection closed while reading" }
        $read += $n
    }
    return $buf
}

function Read-RconPacket($stream) {
    $len = [BitConverter]::ToInt32((Read-Exact $stream 4), 0)
    $payload = Read-Exact $stream $len
    return [pscustomobject]@{
        Id   = [BitConverter]::ToInt32($payload, 0)
        Type = [BitConverter]::ToInt32($payload, 4)
        Body = [System.Text.Encoding]::UTF8.GetString($payload, 8, [Math]::Max(0, $len - 10))
    }
}

function Connect-Rcon([string]$RconHost = "127.0.0.1", [int]$Port = 25575, [string]$Password) {
    $client = New-Object System.Net.Sockets.TcpClient
    $client.Connect($RconHost, $Port)
    $stream = $client.GetStream()
    $stream.ReadTimeout = 20000
    $pkt = New-RconPacket 1 3 $Password
    $stream.Write($pkt, 0, $pkt.Length)
    # A failed auth answers with requestId -1.
    if ((Read-RconPacket $stream).Id -eq -1) { throw "RCON auth failed (check rcon.password)" }
    return [pscustomobject]@{ Client = $client; Stream = $stream }
}

function Invoke-Rcon($Connection, [string]$Command) {
    $pkt = New-RconPacket 2 2 $Command
    $Connection.Stream.Write($pkt, 0, $pkt.Length)
    return (Read-RconPacket $Connection.Stream).Body
}

function Close-Rcon($Connection) {
    if ($Connection -and $Connection.Client) { $Connection.Client.Close() }
}
