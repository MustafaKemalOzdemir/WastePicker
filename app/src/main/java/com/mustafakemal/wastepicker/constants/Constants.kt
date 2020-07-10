package com.mustafakemal.wastepicker.constants

import com.mustafakemal.wastepicker.retrofit.ContainerModel

class Constants {
    companion object{
        const val SHARED_PREF_ID = "global_shared_preferences"
        const val DATA_SYNC_TIME_STAMP = "data_sync_time_stamp"
        const val DATA_SYNC_STRING = "data_sync_string"
        const val NAV_MODE_START_ONLY = 0
        const val NAV_MODE_START_END = 1
        var containerData: List<ContainerModel> ?= null
    }
}