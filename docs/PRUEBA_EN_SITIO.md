# Prueba en sitio: WanderAI (celular) + Vibra (reloj)

Guía para validar la comunicación con un reloj físico (probada pensando en un
Samsung Galaxy Watch7, modelo SM-L310, con Wear OS 5).

## Antes de salir

1. Compila las dos apps **en el mismo computador**: la Data Layer solo comunica apps
   con el mismo `applicationId` (`com.vibra.wanderai`) y la misma firma
   (`~/.android/debug.keystore` en builds debug).
   ```bash
   ./gradlew :app:assembleDebug
   ```
2. Ten el celular con **datos móviles** activos (el chat IA y las rutas necesitan internet;
   el reloj no).
3. Lleva el cable USB del celular y el cargador del reloj.

## 1. Emparejar el reloj con el celular de WanderAI

- Un Galaxy Watch solo se empareja con **un** celular. Si está emparejado con otro, hay
  que restablecerlo de fábrica para emparejarlo con el tuyo (se borran sus datos).
- Alternativa sin restablecer: instalar WanderAI en el celular Android al que ya está
  emparejado el reloj.
- El emparejamiento se hace con la app **Galaxy Wearable** del celular.

## 2. Instalar Vibra en el reloj

1. Reloj: *Ajustes → Acerca del reloj → Información del software* → toca
   *Versión de software* 5 veces.
2. *Opciones de desarrollador*: activa **Depuración ADB** y **Depuración por Wi-Fi**.
3. Conecta el computador y el reloj al **hotspot del celular** (no a la red de la
   universidad) y, con la IP y el puerto que muestra el reloj:
   ```bash
   adb connect <ip-del-reloj>:<puerto>
   adb -s <ip-del-reloj>:<puerto> install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Instala WanderAI en el celular por USB:
   ```bash
   adb -s <serial-del-celular> install -r ../WanderAI/app/build/outputs/apk/debug/app-debug.apk
   ```

## 3. Ajustes del reloj

- Apaga el **Modo Teatro** y el **Modo Dormir** (silencian la vibración).
- Sube la **intensidad de vibración del sistema** (Samsung la aplica sobre la de la app).
- Durante la prueba, apaga el **Wi-Fi del reloj**: así el enlace va por Bluetooth y no
  depende de la red del lugar.

## 4. Diagnóstico

Abre Vibra → *Comenzar Rumbo* → en el inicio, **Diagnóstico**. Debe decir
**"Listo para la prueba"** con:

| Fila | Valor esperado |
|---|---|
| Enlace | Bluetooth |
| Teléfono | El nombre del celular |
| Motor | Con control de fuerza |
| Reloj | Samsung SM-L310 |

- **Probar enlace** debe responder en menos de ~500 ms por Bluetooth.
- **Vibrar al máximo** debe sentirse claramente.

Si aparece "Enlazado por Wi-Fi", acerca el celular o enciende su Bluetooth.

## 5. Prueba funcional

1. En WanderAI, pestaña **Vibra** → *Probar instrucciones*: Izquierda, Recto, Derecha,
   Regresar y Llegada. Cada una tiene un ritmo distinto en el reloj.
2. Chat IA → pide un lugar → *Ver en mapa* → **Fijar rumbo**. El reloj abre la guía solo.
3. En la guía del reloj: **Pausar guía** → el celular muestra "Guía pausada desde el reloj";
   **Reanudar guía** lo quita.
4. Apaga y enciende el Bluetooth del celular unos segundos: el reloj vuelve a enlazarse y,
   si se perdió un aviso hace menos de 60 s, lo recibe al reconectar.

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| "Sin teléfono" en Diagnóstico | Reloj emparejado con otro celular, o WanderAI no instalado/abierto |
| Enlace OK pero no llegan mensajes | Las apps se compilaron en computadores distintos (firmas diferentes) |
| No vibra | Modo Teatro/Dormir, o intensidad del reloj al 0 % |
| La distancia no baja | Falta el permiso de ubicación de WanderAI |
