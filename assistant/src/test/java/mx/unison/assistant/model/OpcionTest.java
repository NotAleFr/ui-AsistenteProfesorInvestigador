package mx.unison.assistant.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OpcionTest {

    @Test
    void testFormateaEnterosSinDecimales() {
        Opcion opcion = new Opcion();
        opcion.setNombreOpcion("Coordinador");
        opcion.setPuntajeOpcion(80.0);

        assertEquals("Coordinador (80 pts)", opcion.getFormattedLabel());
    }

    @Test
    void testFormateaDecimalesSinRedondear() {
        Opcion medioPunto = new Opcion();
        medioPunto.setNombreOpcion("Asistencia");
        medioPunto.setPuntajeOpcion(0.5);

        Opcion cuartoPunto = new Opcion();
        cuartoPunto.setNombreOpcion("Participación");
        cuartoPunto.setPuntajeOpcion(12.25);

        assertEquals("Asistencia (0.5 pts)", medioPunto.getFormattedLabel());
        assertEquals("Participación (12.25 pts)", cuartoPunto.getFormattedLabel());
    }
}
