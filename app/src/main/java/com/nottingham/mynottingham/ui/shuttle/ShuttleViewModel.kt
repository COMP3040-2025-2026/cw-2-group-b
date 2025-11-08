package com.nottingham.mynottingham.ui.shuttle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.data.model.RouteSchedule
import com.nottingham.mynottingham.data.model.ShuttleRoute

/**
 * ViewModel for Shuttle Bus feature
 */
class ShuttleViewModel : ViewModel() {

    private val _routes = MutableLiveData<List<ShuttleRoute>>()
    val routes: LiveData<List<ShuttleRoute>> = _routes

    private val _selectedDayType = MutableLiveData<DayType>(DayType.WEEKDAY)
    val selectedDayType: LiveData<DayType> = _selectedDayType

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        _routes.value = listOf(
            // Route #A: UNM TBS UNM
            ShuttleRoute(
                routeId = "A",
                routeName = "Route A",
                description = "UNM ↔ TBS UNM",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:45pm"),
                    returnToCampus = listOf("7:45am")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:45pm"),
                    returnToCampus = listOf("7:45am")
                ),
                weekendSchedule = null
            ),

            // Route #B: UNM Kj KTM/MRT UNM
            ShuttleRoute(
                routeId = "B",
                routeName = "Route B",
                description = "UNM ↔ Kj KTM/MRT",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:00am", "11:15am",
                        "1:15pm", "1:45pm", "3:15pm", "3:45pm", "5:15pm", "5:45pm",
                        "6:45pm", "7:15pm", "8:45pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:00am", "8:15am", "8:30am", "10:15am",
                        "12:15pm", "2:30pm", "4:30pm",
                        "5:00pm", "6:30pm", "7:30pm", "8:00pm", "9:30pm"
                    )
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:00am", "11:15am",
                        "1:15pm", "1:45pm", "2:45pm", "3:15pm", "3:45pm", "4:15pm",
                        "4:45pm", "5:15pm", "5:45pm", "6:45pm", "7:15pm", "8:45pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:00am", "8:15am", "8:30am", "10:15am",
                        "12:15pm", "2:30pm", "4:30pm",
                        "5:00pm", "6:30pm", "7:30pm", "8:00pm", "9:30pm"
                    )
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "7:30am", "9:30am", "11:30am",
                        "12:30pm", "2:30pm", "3:30pm", "4:30pm", "5:30pm",
                        "6:30pm", "8:30pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:15am", "10:30am", "11:30am",
                        "12:30pm", "2:30pm", "3:15pm", "4:30pm", "5:15pm",
                        "6:30pm", "7:30pm", "9:30pm", "11:30pm"
                    )
                ),
                specialNote = "Pass and stop at MRT Sg Jernih before proceeding to Kajang KTM"
            ),

            // Route #C1: UNM TTS UNM
            ShuttleRoute(
                routeId = "C1",
                routeName = "Route C1",
                description = "UNM ↔ TTS",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:30am (Van)", "10:30am (Van)", "11:30am (Van)",
                        "12:00pm", "12:30pm", "2:30pm", "3:00pm", "4:00pm", "5:00pm",
                        "6:00pm (Bus)", "6:30pm (Bus)", "7:00pm (Bus)",
                        "8:00pm (Bus)", "9:30pm (Bus)", "10:45pm (Bus)", "12:00am (Bus)"
                    ),
                    returnToCampus = listOf(
                        "9:40am (Van)", "10:40am (Van)", "11:40am (Van)",
                        "12:10pm", "12:40pm", "2:40pm", "3:10pm", "4:10pm", "5:10pm",
                        "6:10pm (Bus)", "6:40pm (Bus)", "7:10pm (Bus)",
                        "8:10pm (Bus)", "9:40pm (Bus)", "10:40pm (Bus)"
                    ),
                    vehicleType = "Mixed"
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:30am (Van)", "10:30am (Van)", "11:30am (Van)",
                        "12:00pm", "2:30pm", "3:00pm", "4:00pm", "5:00pm",
                        "6:00pm (Bus)", "6:30pm (Bus)", "7:00pm (Bus)",
                        "8:00pm (Bus)", "9:30pm (Bus)", "10:45pm (Bus)", "12:00am (Bus)"
                    ),
                    returnToCampus = listOf(
                        "9:40am (Van)", "10:40am (Van)", "11:40am (Van)",
                        "12:10pm", "2:40pm", "3:10pm", "4:10pm", "5:10pm",
                        "6:10pm (Bus)", "6:40pm (Bus)", "7:10pm (Bus)",
                        "8:10pm (Bus)", "9:40pm (Bus)", "10:40pm (Bus)"
                    ),
                    vehicleType = "Mixed"
                ),
                weekendSchedule = null
            ),

            // Route #C2: TTS UNM
            ShuttleRoute(
                routeId = "C2",
                routeName = "Route C2",
                description = "TTS → UNM",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = emptyList(),
                    returnToCampus = listOf("8:00am", "8:30am")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = emptyList(),
                    returnToCampus = listOf("8:00am", "8:30am")
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "12:30pm⁷", "2:30pm⁷", "6:45pm⁷", "9:30pm", "11:00pm"
                    ),
                    returnToCampus = listOf(
                        "9:30am", "10:30am",
                        "2:30pm", "6:15pm⁸", "9:15pm⁸", "11:00pm⁸"
                    )
                ),
                specialNote = "⁷Will pass and stop at TTS, then to IOI City Mall\n⁸Will pass and stop at TTS, then to UNM"
            ),

            // Route #D: UNM LOTUS Semenyih UNM
            ShuttleRoute(
                routeId = "D",
                routeName = "Route D",
                description = "UNM ↔ LOTUS Semenyih",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:30pm"),
                    returnToCampus = listOf("9:00pm")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:30pm"),
                    returnToCampus = listOf("9:00pm")
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf("11:30am", "12:30pm"),
                    returnToCampus = listOf("3:15pm", "4:15pm")
                )
            ),

            // Route #E1: UNM Al-Itt'a Mosque TTS (Friday Only)
            ShuttleRoute(
                routeId = "E1",
                routeName = "Route E1",
                description = "UNM ↔ Al-Itt'a Mosque ↔ TTS",
                weekdaySchedule = null,
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("12:45pm", "1:00pm", "1:15pm"),
                    returnToCampus = listOf("2:00pm")
                ),
                weekendSchedule = null,
                specialNote = "Friday Only"
            ),

            // Route #E2: UNM PGA Mosque (Friday Only)
            ShuttleRoute(
                routeId = "E2",
                routeName = "Route E2",
                description = "UNM ↔ PGA Mosque",
                weekdaySchedule = null,
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("12:45pm", "1:00pm", "1:15pm"),
                    returnToCampus = listOf("2:00pm")
                ),
                weekendSchedule = null,
                specialNote = "Friday Only"
            ),

            // Route #G: UNM-IOI
            ShuttleRoute(
                routeId = "G",
                routeName = "Route G",
                description = "UNM ↔ IOI",
                weekdaySchedule = null,
                fridaySchedule = null,
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf("12:30pm⁷", "2:30pm⁷", "6:45pm⁷"),
                    returnToCampus = listOf("5:30pm⁸", "8:30pm⁸", "10:15pm⁸")
                ),
                specialNote = "IOI Service is not available on Public Holidays\n⁷Will pass and stop at TTS, then to IOI City Mall\n⁸Will pass and stop at TTS, then to UNM",
                isActive = true
            )
        )
    }

    fun setDayType(dayType: DayType) {
        _selectedDayType.value = dayType
    }

    fun getScheduleForRoute(routeId: String, dayType: DayType): RouteSchedule? {
        val route = _routes.value?.find { it.routeId == routeId } ?: return null
        return when (dayType) {
            DayType.WEEKDAY -> route.weekdaySchedule
            DayType.FRIDAY -> route.fridaySchedule
            DayType.WEEKEND -> route.weekendSchedule
        }
    }
}
