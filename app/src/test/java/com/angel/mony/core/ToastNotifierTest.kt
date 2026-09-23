package com.angel.mony.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ToastNotifierTest {
    @Test fun `success messages use success tone`() {
        assertEquals(AppToastTone.SUCCESS, toastToneFor("Configuración guardada correctamente."))
        assertEquals(AppToastTone.SUCCESS, toastToneFor("Movimiento eliminado."))
    }

    @Test fun `failure messages use error tone`() {
        assertEquals(AppToastTone.ERROR, toastToneFor("No se pudo guardar el movimiento."))
        assertEquals(AppToastTone.ERROR, toastToneFor("Error al restaurar los datos."))
    }

    @Test fun `neutral messages use info tone`() {
        assertEquals(AppToastTone.INFO, toastToneFor("Este ciclo todavía no ha llegado a su cierre."))
    }
}
