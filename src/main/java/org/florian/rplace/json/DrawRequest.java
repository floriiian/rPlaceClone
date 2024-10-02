package org.florian.rplace.json;

public record DrawRequest(String requestType, String sessionCode, int x, int y, String color, int date) {

}