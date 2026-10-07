<#
  FMCuttingBoard Windows clipboard bridge for the VS Code extension.

  VS Code's clipboard API only handles plain text, but FileMaker uses custom clipboard formats
  (Mac-XMSS, Mac-XMSC, ...). This script reads and writes those formats through .NET. It is a thin
  byte pipe: all encoding and decoding decisions are made in TypeScript (src/core/FmClipboardCodec.ts),
  which is tested against shared/fixtures.

  Invoked as:  powershell.exe -NoProfile -NonInteractive -STA -ExecutionPolicy Bypass -File fmclipboard.ps1 -Mode <mode>

  Modes (all output is a single line of ASCII JSON on stdout; all text and bytes are base64):
    read   -> {"ok":true,"text":<base64 UTF-8 of CF_UNICODETEXT or null>,"formats":[{"name","id","base64"}]}
              formats: only the names passed in -FormatNames (comma-separated), when present
    dump   -> same shape, but formats lists every clipboard format whose data is bytes, a stream or text
    write  -> reads {"text":<base64 UTF-8>,"formats":[{"name","base64"}]} from stdin, sets CF_UNICODETEXT plus
              each format in a single clipboard operation, then {"ok":true}
  Errors: {"ok":false,"error":"<message>"} with exit code 1.
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('read', 'dump', 'write')][string]$Mode,
    [string]$FormatNames = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms

# .NET names for standard formats -> the Win32 names the JetBrains plugin uses in captures
$StandardNames = @{
    'UnicodeText'             = 'CF_UNICODETEXT'
    'Text'                    = 'CF_TEXT'
    'OEMText'                 = 'CF_OEMTEXT'
    'Locale'                  = 'CF_LOCALE'
    'DeviceIndependentBitmap' = 'CF_DIB'
    'Format17'                = 'CF_DIBV5'
    'Bitmap'                  = 'CF_BITMAP'
}

function ToBase64([byte[]]$bytes) {
    if ($null -eq $bytes) { return $null }
    return [Convert]::ToBase64String($bytes)
}

function Write-Json($obj) {
    # -Compress keeps it on one line; base64 keeps it ASCII, so the console code page cannot corrupt it
    [Console]::Out.Write(($obj | ConvertTo-Json -Compress -Depth 5))
}

function Get-FormatBytes($dataObject, [string]$name) {
    $data = $null
    try { $data = $dataObject.GetData($name, $false) } catch { return $null }
    if ($null -eq $data) { return $null }
    if ($data -is [System.IO.MemoryStream]) { return $data.ToArray() }
    if ($data -is [System.IO.Stream]) {
        $ms = New-Object System.IO.MemoryStream
        $data.CopyTo($ms)
        return $ms.ToArray()
    }
    if ($data -is [byte[]]) { return $data }
    if ($data -is [string]) {
        # Text formats come back already decoded; re-encode as the clipboard holds them (UTF-16LE + NUL)
        return [System.Text.Encoding]::Unicode.GetBytes($data + [char]0)
    }
    return $null
}

function Read-Clipboard([bool]$allFormats, [string[]]$wanted) {
    $dataObject = $null
    for ($i = 0; $i -lt 10 -and $null -eq $dataObject; $i++) {
        try { $dataObject = [System.Windows.Forms.Clipboard]::GetDataObject() } catch { Start-Sleep -Milliseconds 50 }
    }
    if ($null -eq $dataObject) { throw 'The clipboard is busy or unavailable.' }

    $text = $null
    if ($dataObject.GetDataPresent([System.Windows.Forms.DataFormats]::UnicodeText, $false)) {
        $s = [string]$dataObject.GetData([System.Windows.Forms.DataFormats]::UnicodeText, $false)
        if ($null -ne $s) { $text = ToBase64([System.Text.Encoding]::UTF8.GetBytes($s)) }
    }

    $formats = @()
    foreach ($name in $dataObject.GetFormats($false)) {
        $include = $allFormats -or ($wanted | Where-Object { $_ -ieq $name })
        if (-not $include) { continue }
        $bytes = Get-FormatBytes $dataObject $name
        if ($null -eq $bytes) { continue }
        $displayName = if ($StandardNames.ContainsKey($name)) { $StandardNames[$name] } else { $name }
        $formats += [ordered]@{
            name   = $displayName
            id     = [System.Windows.Forms.DataFormats]::GetFormat($name).Id
            base64 = ToBase64 $bytes
        }
    }
    return [ordered]@{ ok = $true; text = $text; formats = $formats }
}

function Write-Clipboard() {
    $request = [Console]::In.ReadToEnd() | ConvertFrom-Json
    $dataObject = New-Object System.Windows.Forms.DataObject
    $text = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($request.text))
    $dataObject.SetData([System.Windows.Forms.DataFormats]::UnicodeText, $false, $text)
    foreach ($f in @($request.formats)) {
        $bytes = [Convert]::FromBase64String($f.base64)
        # A MemoryStream is placed on the clipboard as its raw bytes (no .NET serialization header)
        $dataObject.SetData($f.name, $false, (New-Object System.IO.MemoryStream(, $bytes)))
    }
    # copy=$true flushes the data so it survives this process exiting; retry 10 times, 100 ms apart
    [System.Windows.Forms.Clipboard]::SetDataObject($dataObject, $true, 10, 100)
    return [ordered]@{ ok = $true }
}

try {
    switch ($Mode) {
        'read' {
            $wanted = @($FormatNames -split ',' | Where-Object { $_ -ne '' })
            Write-Json (Read-Clipboard $false $wanted)
        }
        'dump' { Write-Json (Read-Clipboard $true @()) }
        'write' { Write-Json (Write-Clipboard) }
    }
    exit 0
} catch {
    Write-Json ([ordered]@{ ok = $false; error = $_.Exception.Message })
    exit 1
}
