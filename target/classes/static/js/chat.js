// Chat widget state
let chatOpen = false;
let isTyping = false;

// Initialize chat on page load
document.addEventListener('DOMContentLoaded', function() {
    loadConversationStarters();

    // Auto-resize textarea
    const chatInput = document.getElementById('chat-input');
    if (chatInput) {
        chatInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 100) + 'px';
        });
    }
});

// Open chat widget
function openChat() {
    chatOpen = true;
    document.getElementById('chat-container').classList.remove('hidden');
    document.getElementById('chat-toggle').classList.add('hidden');
    document.getElementById('chat-input').focus();

    // Scroll to bottom
    scrollToBottom();
}

// Close chat widget
function closeChat() {
    chatOpen = false;
    document.getElementById('chat-container').classList.add('hidden');
    document.getElementById('chat-toggle').classList.remove('hidden');
}

// Get page context
function getPageContext() {
    const path = window.location.pathname;
    let context = {
        pageType: 'home',
        bookId: null
    };

    if (path.includes('/book/')) {
        context.pageType = 'book-detail';
        const parts = path.split('/');
        context.bookId = parts[parts.length - 1];
    } else if (path.includes('/books')) {
        context.pageType = 'books';
    } else if (path.includes('/upload')) {
        context.pageType = 'upload';
    } else if (path.includes('/manage-book')) {
        context.pageType = 'manage-book';
    }

    return context;
}

// Load conversation starters
async function loadConversationStarters() {
    const context = getPageContext();

    try {
        const params = new URLSearchParams();
        if (context.pageType) params.append('pageType', context.pageType);
        if (context.bookId) params.append('bookId', context.bookId);

        const response = await fetch(`/api/chat/starters?${params}`);
        const data = await response.json();

        if (data.success && data.starters) {
            displayStarters(data.starters);
        }
    } catch (error) {
        console.error('Error loading conversation starters:', error);
    }
}

// Display conversation starters
function displayStarters(starters) {
    const startersContainer = document.getElementById('chat-starters');
    startersContainer.innerHTML = '';

    if (starters && starters.length > 0) {
        starters.forEach(starter => {
            const button = document.createElement('button');
            button.className = 'starter-button';
            button.textContent = starter;
            button.onclick = () => {
                document.getElementById('chat-input').value = starter;
                sendMessage();
                startersContainer.classList.add('hidden');
            };
            startersContainer.appendChild(button);
        });
        startersContainer.classList.remove('hidden');
    } else {
        startersContainer.classList.add('hidden');
    }
}

// Handle Enter key press
function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// Send message
async function sendMessage() {
    const input = document.getElementById('chat-input');
    const message = input.value.trim();

    if (!message || isTyping) {
        return;
    }

    // Clear input
    input.value = '';
    input.style.height = 'auto';

    // Hide starters after first message
    document.getElementById('chat-starters').classList.add('hidden');

    // Display user message
    addMessage(message, 'user');

    // Show typing indicator
    showTyping();

    // Get page context
    const context = getPageContext();

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                pageType: context.pageType,
                bookId: context.bookId
            })
        });

        const data = await response.json();

        // Hide typing indicator
        hideTyping();

        if (data.success) {
            addMessage(data.response, 'bot');
        } else {
            addMessage('Sorry, I encountered an error: ' + (data.error || 'Unknown error'), 'bot');
        }

    } catch (error) {
        hideTyping();
        console.error('Error sending message:', error);
        addMessage('Sorry, I could not process your request. Please try again.', 'bot');
    }
}

// Add message to chat
function addMessage(text, sender) {
    const messagesContainer = document.getElementById('chat-messages');

    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message message-${sender}`;

    const bubbleDiv = document.createElement('div');
    bubbleDiv.className = 'message-bubble';
    bubbleDiv.textContent = text;

    messageDiv.appendChild(bubbleDiv);
    messagesContainer.appendChild(messageDiv);

    scrollToBottom();
}

// Show typing indicator
function showTyping() {
    isTyping = true;
    document.getElementById('chat-send').disabled = true;
    const indicator = document.querySelector('#typing-indicator .typing-indicator');
    if (indicator) {
        indicator.classList.add('active');
    }
    scrollToBottom();
}

// Hide typing indicator
function hideTyping() {
    isTyping = false;
    document.getElementById('chat-send').disabled = false;
    const indicator = document.querySelector('#typing-indicator .typing-indicator');
    if (indicator) {
        indicator.classList.remove('active');
    }
}

// Scroll to bottom of messages
function scrollToBottom() {
    const messagesContainer = document.getElementById('chat-messages');
    if (messagesContainer) {
        setTimeout(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }, 100);
    }
}