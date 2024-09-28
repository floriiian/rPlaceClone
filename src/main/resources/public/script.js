
const TRUE_REGEX= /^\s*(true|1|on)\s*$/i;
const socket = new WebSocket("ws://localhost:8888/websocket");

let lastDrawTime = null;
let selectedColor = "#000"; // Save selected color

// TODO: Save last coordinates.+
let lastRequestedColor;
let lastRequestedX;
let lastRequestedY;

const canvasCode = new URLSearchParams(document.location.search).get("canvasCode");

if(canvasCode != null){
    console.log(canvasCode);
}

/* Handlers*/

socket.onopen = function () {
    console.log("Connected to WebSocket server");
};

socket.onclose = function () {
    console.log("Disconnected from WebSocket server");
};

socket.onerror = function (error) {
    console.log("WebSocket Error: " + error);
};

socket.onMessage = function (event) {
    console.log(event.data);

    let jsonData = JSON.parse(event.data);
    let type = jsonData.type;

    if(type === null){
        return;
    }
    switch(type){
        case "sessionResponse":
            console.log(jsonData);
            break;
        case "canvasResponse":
            loadCanvas(jsonData.description);
            break;
        case "canvasUpdate":
            drawRect(jsonData.description);
            break;
        case "drawResponse":
            if(TRUE_REGEX.test(jsonData.description)) {
                drawRect(lastRequestedX, lastRequestedY, lastRequestedColor, 1, 1);
            }
            break;
    }
}

/* Functions used by the handlers */

function sendDrawRequest(userid, color, x, y){

    if(!canDrawAgain()){
        console.log("Draw request failed: Cooldown is still active.");
        return false;
    }

    lastRequestedColor = selectedColor;

    const drawRequest = {
        requestType: "draw",
        color: color,
        position: [x, y],
        date: Math.floor(new Date().getTime() / (1000 * 60))
    };

    socket.send(JSON.stringify(drawRequest))
}

function createNewSession(){
    const request = {
        requestType: "session",
    };
    socket.send(JSON.stringify(request));
}

function canDrawAgain(){
    return lastDrawTime === null ||  new Date().getSeconds() <= lastDrawTime;
}

function loadCanvas(canvasData) {
    console.log(JSON.parse(canvasData));
}

function drawRect(x, y, width, height, color) {
    ctx.fillStyle = color
    ctx.fillRect( x, y, width, height )
}

/* Canvas */

canvas = document.getElementById("canvas");
let ctx = canvas.getContext('2d')

document.addEventListener('click', function (e) {

    const rect = canvas.getBoundingClientRect();
    const elementRelativeX = e.clientX - rect.left;
    const elementRelativeY = e.clientY - rect.top;
    const canvasRelativeX = elementRelativeX * canvas.width / rect.width;
    const canvasRelativeY = elementRelativeY * canvas.height / rect.height;

    console.log(Math.round(canvasRelativeX), Math.round(canvasRelativeY))

    sendDrawRequest(
        null,
        selectedColor,
        Math.round(canvasRelativeX),
        Math.round(canvasRelativeY),
    )
})

setTimeout(createNewSession, 5000);

