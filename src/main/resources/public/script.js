
// Connect to the WebSocket server at ws://localhost:7070/websocket
const socket = new WebSocket("ws://localhost:8888/websocket");

// Triggered when the WebSocket connection is successfully established
socket.onopen = function () {
    console.log("Connected to WebSocket server");
};

// Triggered when a message is received from the WebSocket server
socket.onmessage = function (event) {
    const messageElement = document.createElement("p");
    messageElement.textContent = "Message: " + event.data;
    document.getElementById("messages").appendChild(messageElement);
};

// Triggered when the WebSocket connection is closed
socket.onclose = function () {
    console.log("Disconnected from WebSocket server");
};

// Triggered when an error occurs on the WebSocket
socket.onerror = function (error) {
    console.log("WebSocket Error: " + error);
};

// Function to send a message to the WebSocket server
function sendMessage() {
    socket.send("Hello, server!");
}
canvas = document.getElementById("canvas");
let ctx = canvas.getContext('2d')

document.addEventListener('click', function (e) {

    const rect = canvas.getBoundingClientRect();
    const elementRelativeX = e.clientX - rect.left;
    const elementRelativeY = e.clientY - rect.top;
    const canvasRelativeX = elementRelativeX * canvas.width / rect.width;
    const canvasRelativeY = elementRelativeY * canvas.height / rect.height;

    console.log(Math.round(canvasRelativeX), Math.round(canvasRelativeY))

    drawRect(canvasRelativeX,canvasRelativeY, 1, 1)
})

function drawRect(x, y, width, height) {
    ctx.fillStyle = "rgb(200 0 0)";
    ctx.fillRect( x, y, width, height )
}
