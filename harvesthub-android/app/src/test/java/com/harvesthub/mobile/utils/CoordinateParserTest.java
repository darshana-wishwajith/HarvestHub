package com.harvesthub.mobile.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoordinateParserTest {

    private static void assertLatLng(String input, double lat, double lng) {
        double[] r = CoordinateParser.parse(input);
        assertTrue("no coordinates found in: " + input, r != null);
        assertEquals(lat, r[0], 1e-4);
        assertEquals(lng, r[1], 1e-4);
    }

    @Test
    public void plainDecimalCoordinatesAsCopiedFromGoogleMaps() {
        assertLatLng("8.354611, 80.502111", 8.354611, 80.502111);
        assertLatLng("  8.3546 80.5021 ", 8.3546, 80.5021);
    }

    @Test
    public void degreesMinutesSeconds() {
        assertLatLng("8°21'16.6\"N 80°30'07.6\"E", 8.354611, 80.502111);
    }

    @Test
    public void googleMapsLinks() {
        assertLatLng("https://www.google.com/maps/place/Mihintale/@8.3507,80.5072,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d8.3512!4d80.5104", 8.3512, 80.5104);
        assertLatLng("https://www.google.com/maps/search/?api=1&query=8.354611%2C80.502111", 8.354611, 80.502111);
        assertLatLng("https://maps.google.com/?q=8.3546,80.5021", 8.3546, 80.5021);
        assertLatLng("https://www.google.com/maps/@8.3507,80.5072,17z", 8.3507, 80.5072);
    }

    @Test
    public void placeNamesAndBadInputAreNotCoordinates() {
        assertNull(CoordinateParser.parse("Mihintale"));
        assertNull(CoordinateParser.parse("No 12, Main Street"));
        assertNull(CoordinateParser.parse(""));
        assertNull(CoordinateParser.parse("95.12345, 80.12345"));
    }

    @Test
    public void detectsShortShareLinks() {
        assertTrue(CoordinateParser.isShortMapsLink("https://maps.app.goo.gl/AbCdEf123"));
        assertFalse(CoordinateParser.isShortMapsLink("https://www.google.com/maps/@8.35,80.50,17z"));
    }
}
