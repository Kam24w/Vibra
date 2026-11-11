# Patrones hápticos de Vibra

Un reloj tiene un solo motor, así que el lado o la maniobra se reconocen por el
**ritmo**. Los cuatro comandos principales usan siempre el ritmo del reloj (el mismo
que se aprende en *Test háptico*), aunque WanderAI envíe otro patrón.

Formato: pausa/vibración en milisegundos y fuerza (0–255) de cada pulso.
Definidos en `haptics/HapticWaveform.kt`; probados en `HapticWaveformTest`.

| Comando | Ritmo | Tiempos (ms) | Fuerza | Cómo se siente |
|---|---|---|---|---|
| Izquierda | Pulso rítmico | 160 on · 160 off ×3 | 255 | Tres golpes iguales y regulares |
| Derecha | Doble pulso | (90 on · 70 off · 90 on) · 280 off · (90 on · 70 off · 90 on) | 255 | "ta-ta … ta-ta" |
| Recto | Triple que acelera | 200 on · 240 off · 140 on · 120 off · 90 on | 150 → 200 → 255 | Cada vez más rápido y fuerte |
| Regresar | Alerta intermitente | 3 × (60 on · 60 off) · 300 off · 3 × (60 on · 60 off) | 255 | Dos ráfagas de toques rápidos |
| Llegada | Del celular | 4 × 150 ms | Intensidad del celular | Cuatro pulsos largos |
| Alerta | Del celular | 5 × 100 ms | 255 siempre | Cinco toques fuertes, incluso al 0 % |

## Intensidad

- El ajuste del reloj (0–100 %) escala la fuerza de todos los ritmos; **0 %** apaga el
  motor salvo para las **alertas**.
- En relojes sin control de amplitud solo cambia el ritmo (on/off).
- Las vibraciones usan la categoría **alarma**: suenan con la pantalla apagada y
  atraviesan "No molestar", pero el Modo Teatro/Dormir de Samsung puede silenciarlas.

## Guía en pausa

Con la guía en pausa no vibra ninguna indicación, excepto las **alertas**.
