const loginForm = document.querySelector('#login-form');

const chatBox = document.querySelector('#chat');
const messageControls = document.querySelector('#message-controls');

const colors = document.querySelector('.colors');
const brushes = document.querySelector('.brushes');
const clearButton = document.getElementById('clear-button');

const paintCanvas = document.querySelector('.js-paint');
const context = paintCanvas.getContext('2d');

context.lineCap = 'round';
context.strokeStyle = 'black';
context.lineWidth = 2;

const round = document.querySelector('.round');
const usersPane = document.querySelector('#users');
const readyButton = document.querySelector('#ready-button');
const readyLabel = document.querySelector('.ready-label');

const wordsModalBody = document.querySelector('#word-modal-body');
const turnScoreModalBody = document.querySelector('#turn-score-modal-body');
const gameScoreModalBody = document.querySelector('#game-score-modal-body');

const timer = document.querySelector('.timer');

let stompClient = null;
let username = '';
let x = 0;
let y = 0;
let isMouseDown = false;
let canDraw = false;
let chosenWord = '';
let foundWord = false;

const init = () => {
    x = 0;
    y = 0;
    isMouseDown = false;
    canDraw = false;
    chosenWord = '';
    foundWord = false;

    document.querySelector('.word').innerHTML = '';
    timer.classList.remove('heartbeat-effect');
    timer.innerHTML = '0';
    paintCanvas.classList.remove('paint-canvas-can-draw');
    document.querySelector('.draw-controls').classList.add('disable-elements');

    context.lineCap = 'round';
    context.strokeStyle = 'black';
    context.lineWidth = 2;

    clearCanvas();
};

const stopDrawing = () => {
    isMouseDown = false;
};

const startDrawing = event => {
    isMouseDown = true;
    [x, y] = [event.offsetX, event.offsetY];
    if (canDraw) {
        sendCursor(x, y)
    }
};

const sendCursor = (x, y) => {
    stompClient.send('/app/cursor', {}, JSON.stringify({
        'x': x,
        'y': y
    }));
};

const drawLine = (event) => {
    if (isMouseDown && canDraw) {
        drawLocal(event.offsetX, event.offsetY);
        sendDraw(event.offsetX, event.offsetY);
    }
};

const drawLocal = (newX, newY) => {
    context.beginPath();
    context.moveTo(x, y);
    context.lineTo(newX, newY);
    context.stroke();

    x = newX;
    y = newY;
};

const remoteDraw = (newX, newY, color, thickness) => {
    context.strokeStyle = color;
    context.lineWidth = thickness;
    drawLocal(newX, newY);
};

//-----------socket------------

const handleWordSuggestion = (wordSuggestionEvent) => {
    const words = JSON.parse(wordSuggestionEvent.body).words;

    wordsModalBody.innerHTML = '';

    words.forEach(word => {
        const wordButton = document.createElement('button');
        wordButton.innerHTML = word;
        wordButton.className = 'word-modal-buttons';

        wordButton.addEventListener('click', () => {
            sendChosenWord(word);
        });

        wordsModalBody.append(wordButton);
    });

    $('#word-modal').modal();
};

const sendChosenWord = (word) => {
    stompClient.send('/app/word', {}, JSON.stringify({'word': word}));
    $('#word-modal').modal('hide');
    enableDraw();
    chosenWord = word;
};

const enableDraw = () => {
    canDraw = true;
    paintCanvas.classList.add('paint-canvas-can-draw');
    document.querySelector('.draw-controls').classList.remove('disable-elements');
};

const ready = () => {
    readyButton.classList.add('ready-button-disabled');
    readyButton.blur();
    readyButton.disabled = true;

    stompClient.send('/app/ready', {}, {});

};

const showMainPage = () => {
    const login = document.querySelector('#login');
    login.classList.add('hide');

    const chatPage = document.querySelector('#main-page');
    chatPage.classList.remove('hide');
};

const handleTimeUpdate = (timeEvent) => {
    const turnTime = JSON.parse(timeEvent.body);
    timer.innerHTML = turnTime.time;
    if (turnTime.time <= 10) {
        timer.classList.add('heartbeat-effect');
    }
};

const handleWord = (wordEvent) => {
    const guessedWord = JSON.parse(wordEvent.body);

    if (canDraw) {
        document.querySelector('.word').innerHTML = chosenWord;
    } else if (!foundWord) {
        document.querySelector('.word').innerHTML = guessedWord.word;
    }
};

const handleWordFound = (wordFoundEvent) => {
    const guessedWord = JSON.parse(wordFoundEvent.body);
    document.querySelector('.word').innerHTML = guessedWord.word;
};

const handleEndTurn = (handleEndTurnEvent) => {
    init();

    const endTurn = JSON.parse(handleEndTurnEvent.body);
    createInfoMessage('Turn ended', endTurn.time);


    turnScoreModalBody.innerHTML = '';

    const word = document.createElement('div');
    word.className = 'd-flex justify-content-center mb-1 end-turn-word';
    word.innerHTML = 'El kelma kenet&nbsp;<b> ' + endTurn.word + ' </b>';
    turnScoreModalBody.appendChild(word);

    endTurn.stats.forEach((stat, rank) => {
        const userBox = document.createElement('div');
        userBox.className = 'd-flex justify-content-start end-turn-user';
        userBox.innerHTML = (rank + 1) + ' - ' + stat.username + ' (' + stat.score + ')';
        turnScoreModalBody.appendChild(userBox);
    });

    $('#turn-score-modal').modal();

    setTimeout(function () {
        $('#turn-score-modal').modal('hide');
    }, 5000);
};

const handleStartRound = (handleStartRoundEvent) => {
    const startRound = JSON.parse(handleStartRoundEvent.body);

    createInfoMessage('Round <b>' + startRound.currentRound + '</b> started', startRound.time);
    round.innerHTML = startRound.currentRound + " / " + startRound.maximumRound;
};

const handleEndRound = (handleEndRoundEvent) => {
    const endRound = JSON.parse(handleEndRoundEvent.body);
    createInfoMessage('Round ended', endRound.time);
};

const handleEndGame = (handleEndGameEvent) => {
    const endGame = JSON.parse(handleEndGameEvent.body);

    reset();

    endGame.stats.forEach((stat, rank) => {
        createInfoMessage('Game ended', endGame.time);
        const userBox = document.createElement('div');
        userBox.className = 'd-flex justify-content-start end-turn-user';

        let content = (rank + 1) + ' - ' + stat.username + ' (' + stat.score + ')';
        switch (rank + 1) {
            case 1:
                content += '<i class="fas fa-crown end-game-first"></i>';
                break;
            case 2:
                content += '<i class="fas fa-crown end-game-second"></i>';
                break;
            case 3:
                content += '<i class="fas fa-crown end-game-third"></i>';
                break;
        }

        userBox.innerHTML = content;
        gameScoreModalBody.appendChild(userBox);
    });

    $('#game-score-modal').modal();

    setTimeout(function () {
        $('#game-score-modal').modal('hide');
    }, 7000);


};

const registerRoutes = (username) => {
    const socket = new SockJS('/draw-guess');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, () => {

        stompClient.subscribe('/topic/messages', showMessage);
        stompClient.subscribe('/topic/draw', handleRemoteDrawMessage);
        stompClient.subscribe('/topic/cursor', handleCursor);
        stompClient.subscribe('/topic/clear', handleClear);
        stompClient.subscribe('/topic/users', handleUpdateUsers);
        stompClient.subscribe('/topic/time', handleTimeUpdate);
        stompClient.subscribe('/topic/word', handleWord);
        stompClient.subscribe('/topic/start-round', handleStartRound);
        stompClient.subscribe('/topic/end-turn', handleEndTurn);
        stompClient.subscribe('/topic/end-round', handleEndRound);
        stompClient.subscribe('/topic/end-game', handleEndGame);

        stompClient.subscribe('/user/topic/word-found', handleWordFound);
        stompClient.subscribe('/user/topic/words-suggestion', handleWordSuggestion);

        stompClient.send('/app/connect', {}, JSON.stringify({'username': username}));
        showMainPage();
    });
};

const connect = (event) => {
    username = document.getElementById('username').value;

    fetch('http://localhost:8080/login?username=' + username)
        .then(response => response.json())
        .then(data => {
            if (data.error === false) {
                registerRoutes(username);
            } else {
                alert(data.message);
            }
        });


    event.preventDefault();
    return false;
};

const handleUpdateUsers = (usersEvent) => {
    const users = JSON.parse(usersEvent.body);

    let readyUsers = 0;

    usersPane.innerHTML = '';

    users.forEach((user, rank) => {
        const flexBox = document.createElement('div');

        let classes = 'd-flex justify-content-start user-score-line mb-1';
        if (user.username === username) {
            classes += ' current-user';
        }
        flexBox.className = classes;

        let content = (rank + 1) + ' - ' + user.username + ' (' + user.score + ')';

        if (user.currentTurnScore && user.currentTurnScore > 0) {
            content += ' +' + user.currentTurnScore;
        }

        if (user.drew) {
            content += ' <i class="fas fa-pencil-alt user-drew"></i>';
        } else if (user.drawing) {
            content += ' <i class="fas fa-pencil-alt user-drawing"></i>';
        }

        flexBox.innerHTML = content;

        usersPane.appendChild(flexBox);

        if (user.ready) {
            readyUsers++
        }
    });

    readyLabel.innerText = readyUsers + '/' + users.length + ' 7ather(in)'
};

const disconnect = () => {
    if (stompClient != null) {
        stompClient.disconnect();
    }
};

//-----------draw------------
const handleCursor = (cursorEvent) => {
    const cursor = JSON.parse(cursorEvent.body);

    if (cursor.username !== username) {
        x = cursorEvent.x;
        y = cursorEvent.y;
    }
};

function sendDraw(newX, newY) {
    const data = {
        'newX': newX,
        'newY': newY,
        'color': context.strokeStyle,
        'thickness': context.lineWidth
    };

    stompClient.send('/app/draw', {}, JSON.stringify(data));
}

const handleRemoteDrawMessage = (drawEvent) => {
    const draw = JSON.parse(drawEvent.body);

    if (draw.username !== username) {
        remoteDraw(draw.newX, draw.newY, draw.color, draw.thickness);
    }
};

const sendClear = () => {
    stompClient.send('/app/clear', {}, JSON.stringify({'clear': true}));
};

const handleClear = (clearEvent) => {
    if (JSON.parse(clearEvent.body).username !== username) {
        clearCanvas();
    }
};

const clear = () => {
    clearCanvas();
    sendClear();
};

const clearCanvas = () => {
    context.clearRect(0, 0, paintCanvas.width, paintCanvas.height);
};

//-----------chat------------
const sendMessage = (event) => {
    const messageInput = document.querySelector('#message');

    const chatMessage = {
        text: messageInput.value,
    };
    stompClient.send('/app/chat', {}, JSON.stringify(chatMessage));
    messageInput.value = '';

    event.preventDefault();
};

const resetReadyButton = () => {
    readyButton.classList.remove('ready-button-disabled');
    readyButton.disabled = false;
};

const reset = () => {
    resetReadyButton();
    init();
    $('#word-modal').modal('hide');
    $('#turn-score-modal').modal('hide');
};

const showMessage = (messageEvent) => {
    const message = JSON.parse(messageEvent.body);

    switch (message.type) {
        case 'STARTING':
            createInfoMessage(message.text, message.time);
            break;
        case 'FOUND':
            createInfoMessage('AAAAA <b>' + message.username + '</b> L9AHA!!.', message.time);
            break;
        case 'DRAWING':
            createInfoMessage('Haw <b>' + message.username + '</b> bech ysawer.', message.time);
            clearCanvas();
            break;
        case 'CONNECT':
            createInfoMessage('<b>' + message.username + '</b> connecta, mar7bé!', message.time);
            break;
        case 'DISCONNECT':
            createInfoMessage('<b>' + message.username + '</b> 5raj!', message.time);
            break;
        case 'READY':
            createInfoMessage('Aya haw <b>' + message.username + '</b> 7thar!', message.time);
            break;
        default:
            createChatMessage(message.text, message.time, message.username);
            break;
    }
};

const createAvatarElement = (username) => {
    const avatarText = document.createTextNode(username[0]);

    const avatarElement = document.createElement('div');
    avatarElement.className = 'circle';
    avatarElement.style['background-color'] = getAvatarColor(username);
    avatarElement.appendChild(avatarText);

    const avatarContainer = document.createElement('div');
    avatarContainer.className = 'img-cont-msg';

    avatarContainer.appendChild(avatarElement);
    return avatarContainer;
};

const createMessageElement = (message, time, username) => {
    const timeElement = createTimeElement(time);

    const messageElement = document.createElement('div');
    messageElement.innerHTML = message;
    messageElement.appendChild(timeElement);

    let classes = 'msg-container-send';
    if (username) {
        messageElement.style['background-color'] = getAvatarColor(username);
    } else {
        classes += ' msg-container-send-info';
    }
    messageElement.className = classes;

    return messageElement;
};

const createChatMessage = (message, time, username) => {
    const avatarContainer = createAvatarElement(username);
    const messageElement = createMessageElement(message, time, username);

    const infoMessageBox = document.createElement('div');
    infoMessageBox.className = 'd-flex justify-content-start mb-3';
    infoMessageBox.appendChild(avatarContainer);
    infoMessageBox.appendChild(messageElement);

    chatBox.appendChild(infoMessageBox);

    scrollDownChat()
};

const createInfoMessage = (message, time) => {
    const messageElement = createMessageElement(message, time);

    const infoMessageBox = document.createElement('div');
    infoMessageBox.className = 'd-flex justify-content-end mb-2';
    infoMessageBox.appendChild(messageElement);

    chatBox.appendChild(infoMessageBox);

    scrollDownChat();
};

const scrollDownChat = () => {
    chatBox.scrollTop = chatBox.scrollHeight;
    const chatContainer = document.querySelector('.chat-container');
    chatContainer.scrollTop = chatContainer.scrollHeight;
};

const createTimeElement = (time) => {
    const timeElement = document.createElement('span');

    timeElement.className = 'msg-time-send';
    timeElement.innerHTML = time;

    return timeElement;
};

//-----------utils------------

// limit the number of events per second
function throttle(callback, delay) {
    let previousCall = new Date().getTime();
    return function () {
        let time = new Date().getTime();

        if ((time - previousCall) >= delay) {
            previousCall = time;
            callback.apply(null, arguments);
        }
    };
}

const hashCode = (str) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
    }
    return hash
};

const getAvatarColor = (messageSender) => {
    const colours = ['#2196F3', '#32c787', '#1BC6B4', '#A1B4C4'];
    const index = Math.abs(hashCode(messageSender) % colours.length);
    return colours[index];
};

function colorChanged(event) {
    context.strokeStyle = event.target.value || 'black';
}

function thicknessChanged(event) {
    context.lineWidth = event.target.value || 1;
}


loginForm.addEventListener('submit', connect, true);

paintCanvas.addEventListener('mousedown', startDrawing);
paintCanvas.addEventListener('mousemove', throttle(drawLine, 10), false);

paintCanvas.addEventListener('mouseup', stopDrawing);
paintCanvas.addEventListener('mouseout', stopDrawing);

colors.addEventListener('click', colorChanged);
brushes.addEventListener('click', thicknessChanged);

clearButton.addEventListener('click', clear);

messageControls.addEventListener('submit', sendMessage, true);

readyButton.addEventListener('click', ready);

init();