$root = "D:\Zahen\dev-tech\new\Minecraft custom mods\Bloodline SMP\resource-pack-local\Blades Of Majestica OmniBlade Addon"
$sourceRoot = "D:\Zahen\dev-tech\new\Minecraft custom mods\Bloodline SMP\resource-pack-source\Blades Of Majestica"
$itemFile = Join-Path $sourceRoot "assets\minecraft\items\netherite_sword.json"

New-Item -ItemType Directory -Force -Path (Join-Path $root "assets\minecraft\items") | Out-Null

$packMeta = @'
{
  "pack": {
    "description": "Bloodline SMP OmniBlade addon for Blades of Majestica",
    "supported_formats": [63, 88.0],
    "pack_format": 88.0,
    "min_format": 63,
    "max_format": 88.0
  }
}
'@

Set-Content -Path (Join-Path $root "pack.mcmeta") -Value $packMeta -Encoding UTF8

$content = Get-Content -Raw $itemFile

if ($content -notmatch '"when": "OmniBlade"') {
    $needle = @'
	  {
        "when": "Soul Collector",
        "model": { "type": "minecraft:model", "model": "minecraft:item/soul_collector" }    
	  },
'@

    $insert = @'
	  {
        "when": "Soul Collector",
        "model": { "type": "minecraft:model", "model": "minecraft:item/soul_collector" }    
	  },
	  {
        "when": "OmniBlade",
        "model": { "type": "minecraft:model", "model": "minecraft:item/soul_collector" }    
	  },
'@

    $content = $content.Replace($needle, $insert)
}

Set-Content -Path (Join-Path $root "assets\minecraft\items\netherite_sword.json") -Value $content -Encoding UTF8

$zipPath = "D:\Zahen\dev-tech\new\Minecraft custom mods\Bloodline SMP\resource-pack-local\Blades-Of-Majestica-OmniBlade-Addon.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Compress-Archive -Path (Join-Path $root '*') -DestinationPath $zipPath

Write-Output "Created: $root"
Write-Output "Zip: $zipPath"
