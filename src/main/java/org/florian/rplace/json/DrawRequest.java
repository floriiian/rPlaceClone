package org.florian.rplace.json;

public record DrawRequest(String requestType, String sessionCode, int[] position, String color, int date) {

}