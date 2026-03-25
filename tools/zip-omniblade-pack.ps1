$zipPath = "D:\Zahen\dev-tech\new\Minecraft custom mods\Bloodline SMP\resource-pack-local\OmniBlade-Resource-Pack.zip"
$sourcePath = "D:\Zahen\dev-tech\new\Minecraft custom mods\Bloodline SMP\resource-pack-local\OmniBlade Resource Pack\*"

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Compress-Archive -Path $sourcePath -DestinationPath $zipPath
Write-Output $zipPath
