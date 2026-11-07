package com.vibra.navigation.data.wear

/**
 * Contrato de la Wear OS Data Layer compartido con WanderAI.
 * Cualquier cambio aquí debe reflejarse en la app del teléfono.
 */
object DataLayerContract {
    // DataClient
    const val PATH_ACTIVE_ROUTE = "/wanderai/active_route"
    const val KEY_CURRENT_DESTINATION = "current_destination"
    const val KEY_REMAINING_DISTANCE = "remaining_distance"
    const val KEY_ROUTE_ACTIVE = "route_active"

    // MessageClient
    const val PATH_NAVIGATION_EVENT = "/wanderai/navigation_event"
    const val PATH_USER_ACTION = "/vibra/user_action"

    /**
     * Capability que debe declarar WanderAI en `res/values/wear.xml` del teléfono.
     * Si no la declara, se usa como respaldo cualquier nodo conectado.
     */
    const val CAPABILITY_WANDERAI = "wanderai_navigation"
}
