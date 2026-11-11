# Vibra — navegación háptica para Wear OS

Vibra es la app de reloj que acompaña a **WanderAI** (celular). WanderAI calcula la
ruta y envía cada indicación al reloj; Vibra la traduce en un **ritmo de vibración**
distinto para cada maniobra, para caminar sin mirar la pantalla.

## Funciones

- **Guía háptica:** izquierda, derecha, recto, regresar y llegada, cada una con su ritmo.
- **Presentación y búsqueda del celular** con radar animado.
- **Test háptico** para aprender los ritmos y **ajuste de intensidad** de 0 a 100 %.
- **Diagnóstico** para la prueba en sitio: enlace (Bluetooth / Wi-Fi / túnel), celular,
  motor de vibración y una prueba de ida al celular.
- **Actividad en curso** y **Tile** para volver a la guía desde la esfera.
- **Modo ambiente:** con la pantalla atenuada, la guía sigue visible en bajo consumo.
- Español e inglés.

## Arquitectura

```
presentation/   Pantallas Compose para Wear OS (Material 3) y sus ViewModels
  app/          Grafo de navegación (SwipeDismissableNavHost)
  splash, link, home, guidance, haptics, diagnostics, ambient
data/
  repository/   WearNavigationRepository: Data Layer (Bluetooth) con reintentos
  wear/         Contrato con WanderAI y puente de depuración por túnel ADB
  parser/       JSON de los eventos de navegación (kotlinx.serialization)
  settings/     Intensidad háptica persistente
haptics/        Ritmos (HapticWaveform) y motor (VibrationManager)
service/        WearableListenerService: recibe eventos con la app cerrada
ongoing/        Actividad en curso de la ruta
tile/           Tile de Wear OS (ProtoLayout)
di/             Contenedor de dependencias manual
```

## Compilar y probar

```bash
./gradlew :app:assembleDebug          # APK debug
./gradlew :app:testDebugUnitTest      # tests unitarios (JVM)
./gradlew :app:connectedDebugAndroidTest   # tests de interfaz en un reloj/emulador
```

> WanderAI y Vibra comparten `applicationId` (`com.vibra.wanderai`). La Data Layer solo
> comunica apps con **el mismo id y la misma firma**: compila las dos en el mismo equipo
> (debug) o con el mismo keystore (release, ver `app/build.gradle.kts`).

## Más documentación

- [Prueba en sitio con un Galaxy Watch](docs/PRUEBA_EN_SITIO.md)
- [Protocolo con WanderAI](docs/PROTOCOLO.md)
- [Patrones hápticos](docs/PATRONES_HAPTICOS.md)
- Túnel ADB para probar con el emulador: `tools/dev-tunnel.ps1`
