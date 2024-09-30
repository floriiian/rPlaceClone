
const socket = new WebSocket("ws://localhost:8888/canvas");
const startURL = "http://localhost:63342/rPlaceClone/src/main/resources/public/client.html"

let lastDrawTime = null;
let selectedColor = "#000"; // Save selected color
const canvasCode = new URLSearchParams(document.location.search).get("canvasCode");

const interval = 20000;
let pingInterval;
let isCanvasLoaded = false;

const canvasContainer = document.getElementById("canvas-container");
const preLoader = document.getElementById("preloader_img");
const selectedPixelImage = document.getElementById("selected_pixel")
const canvas = document.getElementById("canvas");

/* Handlers*/

socket.onopen = function () {
    console.log("Connected to Canvas: " + canvasCode);

    pingInterval = setInterval(() => {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send("0");
        }
    }, interval);

};

socket.onclose = function () {

    clearInterval(pingInterval);
    console.log("Disconnected from Canvas: " + canvasCode);
};

socket.onerror = function (error) {
    console.log("Canvas-socket Error: " + error);
};

socket.onmessage = function (event) {

    let jsonData = JSON.parse(event.data);
    let type = jsonData.type;

    if(type === null){
        return;
    }

    switch(type){
        /* Initializes a session, if not connecting to an existing one.*/
        case "sessionResponse":

            let canvasCode = jsonData.canvasCode;

            if(canvasCode == null){
                return;
            }

            let url = new URL(startURL);
            let params = new URLSearchParams(url.search);

            params.set("canvasCode", canvasCode);
            url.search = params.toString();

            window.location.href = url.toString();

            break;
        /* Receives canvas that needs to be drawn*/
        case "canvasResponse":
            loadCanvas(jsonData);
            break;
        /* Updates the canvas, whenever a new pixel is placed.*/
        case "canvasUpdate":
            console.log("canvas has been updated");
            drawRect(
                jsonData.position[0]
                ,jsonData.position[1],
                jsonData.color
            );
            break;
    }
}

/* Functions used by the handlers */

function sendDrawRequest(userid, color, x, y){

    if(!canDrawAgain()){
        console.log("Draw request failed: Cooldown is still active.");
        return false;
    }

    const drawRequest = {
        requestType: "draw",
        color: color,
        position: [x, y],
        date: Math.floor(new Date().getTime() / (1000 * 60))
    };

    socket.send(JSON.stringify(drawRequest))
}

function sendCanvasRequest(){
    const canvasRequest = {
        requestType: "canvas",
        canvasCode: canvasCode,
    };
    socket.send(JSON.stringify(canvasRequest))
}

function createNewSession(){
    const request = {
        requestType: "session",
    };
    socket.send(JSON.stringify(request));
}

function canDrawAgain(){
    return lastDrawTime === null || (new Date().getTime() - lastDrawTime) > 5000;
}

function loadCanvas(canvasData) {
    const positions = JSON.parse(canvasData.canvasData);
    try{
        for(let i = 0; i < positions.length; i++) {
            let data = positions[i];
            let x = data["position"][0];
            let y = data["position"][1];
            let color = data["color"];

            drawRect(x, y, color);
        }
        preLoader.style.display = "none";
        canvasContainer.style.display = "fixed";
        isCanvasLoaded = true;
    }
    catch(e){
        console.log("Could not load pixel: " + e);
    }
}

function drawRect(x, y, color) {
    ctx.fillStyle = color
    ctx.fillRect( x, y, 1, 1 )
}

/* Canvas */

let ctx = canvas.getContext('2d')

canvas.addEventListener('click', function (e) {
    if(!isDragging && isCanvasLoaded){
        const rect = canvas.getBoundingClientRect();

        const elementRelativeX = e.clientX - rect.left;
        const elementRelativeY = e.clientY - rect.top;

        const canvasRelativeX = elementRelativeX * (canvas.width / rect.width);
        const canvasRelativeY = elementRelativeY * (canvas.height / rect.height);

        /* Uthis logic after confirming pixel-placement
         sendDrawRequest(
            null,
            selectedColor,
            Math.round(canvasRelativeX),
            Math.round(canvasRelativeY)
        );*/

        const pixelSize = 25; // Size of ogn. Image.
        selectedPixelImage.style.left = (rect.left + elementRelativeX - pixelSize / 2) + 'px';
        selectedPixelImage.style.top = (rect.top + elementRelativeY - pixelSize / 2) + 'px';
    }
});

let scaleFactor = 1;
let initialOffsetX = 0;
let initialOffsetY = 0;

let isZooming = false;
let targetScaleFactor;

canvasContainer.onwheel = zoom;

function zoom(event) {
    event.preventDefault();

    targetScaleFactor = scaleFactor + event.deltaY * -0.01;
    targetScaleFactor = Math.min(Math.max(1, targetScaleFactor), 10);

    isZooming = true;
    requestAnimationFrame(zoomAnimation);
}

function zoomAnimation() {
    if (!isZooming) return;

    scaleFactor += (targetScaleFactor - scaleFactor) * 0.1;

    canvasContainer.style.transform = `scale(${scaleFactor}) translate(${initialOffsetX}px, ${initialOffsetY}px)`;

    if (Math.abs(targetScaleFactor - scaleFactor) > 0.01) {
        requestAnimationFrame(zoomAnimation);
    } else {
        isZooming = false;
    }
}

let isDragging = false;
let startX, startY;
let offsetX = 0, offsetY = 0;

canvasContainer.addEventListener('mousedown', (e) => {
    isDragging = true;
    startX = e.clientX;
    startY = e.clientY;
});

document.addEventListener('mousemove', (e) => {
    if (!isDragging) return;

    offsetX = (e.clientX - startX) / scaleFactor;
    offsetY = (e.clientY - startY) / scaleFactor;

    const deadZone = 10;
    if (Math.abs(offsetX) < deadZone && Math.abs(offsetY) < deadZone) {
        return;
    }

    canvasContainer.style.transform = `scale(${scaleFactor}) translate(${initialOffsetX + offsetX}px, ${initialOffsetY + offsetY}px)`;
});

document.addEventListener('mouseup', () => {
    isDragging = false;

    initialOffsetX += offsetX;
    initialOffsetY += offsetY;
});
window.addEventListener('load', function() {
    if(canvasCode != null){
        setTimeout(function () {
            if(socket.readyState === WebSocket.OPEN){
                sendCanvasRequest();
            }
        }, 5000);
    }
})


