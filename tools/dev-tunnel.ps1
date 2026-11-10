<#
.SYNOPSIS
  Abre el túnel ADB de pruebas locales entre WanderAI (teléfono) y Vibra (reloj emulado).

.DESCRIPTION
  Cuando Google Play Services del reloj emulado no empareja con el teléfono
  ("Got 0 accounts"), las builds debug usan un puente TCP:
    - WanderAI escucha en 127.0.0.1:5650 del teléfono (LocalDevBridge).
    - Vibra se conecta a 10.0.2.2:5650 desde el emulador (LocalDevBridgeClient).
  Este script:
    1. detecta qué dispositivo conectado es el reloj y cuál el teléfono;
    2. ejecuta `adb -s <teléfono> forward tcp:5650 tcp:5650`;
    3. concede ACCESS_LOCAL_NETWORK al reloj (Android 17+ descarta el tráfico a 10.0.2.2 sin él).
  Con un reloj físico emparejado no hace falta: se usa la Data Layer por Bluetooth.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\dev-tunnel.ps1
#>
param(
    [int]$Port = 5650,
    [string]$Package = "com.vibra.wanderai"
)

$ErrorActionPreference = "Stop"

$serials = adb devices | Select-String -Pattern "^(\S+)\s+device$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
if (-not $serials) { throw "No hay dispositivos ADB conectados." }

$watch = $null
$phones = @()
foreach ($s in $serials) {
    $traits = (adb -s $s shell getprop ro.build.characteristics).Trim()
    if ($traits -match "watch") { $watch = $s } else { $phones += $s }
}

if (-not $phones) { throw "No se encontró un teléfono (WanderAI) entre: $($serials -join ', ')" }
if ($phones.Count -gt 1) { Write-Warning "Varios teléfonos conectados; se usa $($phones[0])." }
$phone = $phones[0]

adb -s $phone forward "tcp:$Port" "tcp:$Port" | Out-Null
Write-Host "Túnel listo: teléfono $phone 127.0.0.1:$Port <- host:$Port (reloj: 10.0.2.2:$Port)"

if ($watch) {
    adb -s $watch shell pm grant $Package android.permission.ACCESS_LOCAL_NETWORK 2>$null | Out-Null
    Write-Host "Reloj $watch : permiso ACCESS_LOCAL_NETWORK concedido (si la build lo declara)."
} else {
    Write-Warning "No se detectó un reloj; arranca el emulador de Wear OS y vuelve a ejecutar el script."
}

Write-Host "Abre WanderAI en el teléfono y Vibra en el reloj: en ~2 s el reloj muestra 'WanderAI (puente ADB)'."
