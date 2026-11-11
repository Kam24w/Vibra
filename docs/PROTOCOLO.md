# Protocolo WanderAI ↔ Vibra

La comunicación usa la **Wear OS Data Layer** de Google Play Services (Bluetooth, o
Wi-Fi/nube si el celular está lejos). Ambas apps tienen `applicationId`
`com.vibra.wanderai` y deben estar firmadas con la misma clave.

Contrato en código: `data/wear/DataLayerContract.kt` (reloj) y `WearProtocol.kt` (celular).
Los tests `WanderAIContractTest` (reloj) y `WearProtocolTest` (celular) fallan si cambia.

## Celular → reloj

### `/wanderai/navigation_event` (MessageClient, JSON UTF-8)

```json
{"action":"U_TURN","instruction":"Da la vuelta","distance_meters":0,
 "vibration_pattern":{"pulses":3,"duration_ms":200,"amplitude":255},
 "timestamp":1778344620000}
```

| `action` | Significado | Ritmo en el reloj |
|---|---|---|
| `TURN_LEFT` | Girar a la izquierda | Propio del reloj |
| `TURN_RIGHT` | Girar a la derecha | Propio del reloj |
| `STRAIGHT` | Seguir recto | Propio del reloj |
| `U_TURN` | Regresar | Propio del reloj |
| `ARRIVED` | Llegada | `vibration_pattern` del celular |
| `ALERT` | Aviso urgente ("Buscar mi Vibra") | `vibration_pattern`, siempre a máxima fuerza |

- Acciones desconocidas llegan como `UNKNOWN` (compatibilidad hacia adelante).
- Eventos con `timestamp` igual o anterior al último se ignoran (duplicados).
- Eventos con más de 30 s se muestran pero no vibran.

### `/wanderai/active_route` (DataClient, DataMap)

| Clave | Tipo | Ejemplo |
|---|---|---|
| `current_destination` | String | `La Casita del Cuy` |
| `remaining_distance` | String | `850 m`, `1.4 km` |
| `route_active` | Boolean | `true` |

Es persistente: Play services lo sincroniza al reconectar.

## Reloj → celular

### `/vibra/user_action` (MessageClient, JSON)

```json
{"action":"PAUSE_GUIDANCE","timestamp":1778344620000}
```

`REPEAT_INSTRUCTION`, `PAUSE_GUIDANCE`, `RESUME_GUIDANCE`, `STOP_NAVIGATION`,
`ACKNOWLEDGE` (el ping del diagnóstico).

## Capabilities

- Celular: `wanderai_navigation` (el reloj lo prefiere al elegir nodo).
- Reloj: `vibra_wear_client`.

## Reconexión

- El reloj reenvía la última acción con estado (pausa/reanudar/detener) al reconectar.
- El celular reenvía la última indicación perdida si tiene menos de 60 s.
- Puente de depuración (solo emulador): mismos paths sobre TCP `10.0.2.2:5650`, con
  latido cada 3 s (`/bridge/ping`) y corte a los 10 s sin datos.
