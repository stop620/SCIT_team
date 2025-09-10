// 전역 변수
let player; // 유튜브 플레이어
let timelineTimeout;
const timelines = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let subtitleInterval;
let liked = false;
let likeCount = 0;
let currentSectionStart = 0; // 현재 섹션 시작 시간 저장 변수 추가

// 임시 말하기 기능 전역 변수
const startButton = document.getElementById('startButton');
const stopButton = document.getElementById('stopButton');
let mediaRecorder;
let audioChunks = [];

// 언어 바꾸기 전역 변수
const langChange = document.getElementById('langChange');

// --- 단어 선택 관련 전역 변수 ---
let isSelecting = false;
let selectionStartTokenIndex = -1;
let selectionEndTokenIndex = -1;
let globalClickListener = null;
let isWordSelectorActive = false;
let selectedWordList = [];

function renderCard(cardData) {
    document.getElementById('cardTitle').innerText = cardData.title || "";
    document.getElementById('cardLike').innerText = cardData.like || "";

    const tagsContainer = document.querySelector('.tags.card');
    tagsContainer.innerHTML = ''; // 기존 태그 초기화

    // level 값 숫자 -> 한글 매핑
    const levelMap = {
        1: "초급",
        2: "중급",
        3: "고급"
    };

    if (cardData.level) {
        const levelText = levelMap[cardData.level] || "기본";
        const levelDiv = document.createElement('div');
        levelDiv.className = 'tag-item';
        levelDiv.textContent = levelText;
        tagsContainer.appendChild(levelDiv);
    }

    if (cardData.tag) {
        const tagList = cardData.tag.split(',').map(tag => tag.trim());
        tagList.forEach(tag => {
            if (tag) {
                const tagDiv = document.createElement('div');
                tagDiv.className = 'tag-item';
                tagDiv.textContent = tag;
                tagsContainer.appendChild(tagDiv);
            }
        });
    }
}

$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const cardId = urlParams.get('cardId');

    console.log("🏷️ URL에서 추출한 cardId:", cardId);

    function updateLikeButton() {
        $('#cardLike').text(card.like);
        if (liked) {
            $('#like-button').css('color', 'red');
        } else {
            $('#like-button').css('color', 'black');
        }
    }

    if (cardId) {
        $.get(`/mochilearn/api/study/card?cardId=${cardId}`)
            .done(function(cardData) {
                card = cardData;
                card.videoId = extractVideoId(card.url);
                renderCard(card);
                renderSectionButtons(card.sections);

                liked = card.liked;
                updateLikeButton();

                if (typeof YT !== 'undefined' && YT && YT.Player) {
                    createPlayer(card.videoId);
                } else {
                    console.warn("⚠️ YouTube IFrame API가 아직 로드되지 않음");
                }
            })
            .fail(function(jqXHR, textStatus, errorThrown) {
                console.error("❌ API 호출 실패:", textStatus, errorThrown);
            });

        let isLoggedIn = false;

        // 페이지 로드시 로그인 상태 확인 API 호출
        $.get('/mochilearn/api/user/session')
            .done(function(userData) {
                isLoggedIn = !!(userData && userData.loggedIn);
            })
            .fail(function() {
                isLoggedIn = false;
            });

        // 좋아요 클릭 이벤트
        $('#like-button').click(function() {
            if (!isLoggedIn) {
                alert('좋아요를 누르려면 로그인해야 합니다.');
                return;
            }

            const newLiked = !liked;

            $.post('/mochilearn/api/study/likes/toggle', { cardId: cardId, liked: newLiked })
                .done(function(response) {
                    if ((response.status === 'liked' && newLiked) || (response.status === 'unliked' && !newLiked)) {
                        liked = newLiked;
                        card.like += liked ? 1 : -1;
                        $('#cardLike').text(card.like);
                        updateLikeButton();
                    } else {
                        alert('좋아요 상태 갱신 실패');
                    }
                })
                .fail(function() {
                    alert('좋아요 처리 중 오류');
                });
        });

        // 삭제 버튼 이벤트
        $('#delete-button').click(function() {
            if (!confirm("카드를 삭제하시겠습니까?")) return;
            $.ajax({
                url: `/mochilearn/api/study/card/${cardId}`,
                type: 'DELETE',
                success: function() {
                    alert("카드가 삭제되었습니다.");
                    window.location.href = "/mochilearn/page/study";
                },
                error: function() {
                    alert("카드 삭제에 실패했습니다.");
                }
            });
        });

        // 자막 버튼 이벤트
        $('#prev-btn').click(() => changeTranscriptIndex(-1));
        $('#next-btn').click(() => changeTranscriptIndex(1));
    } else {
        console.warn("⚠️ URL에 cardId 파라미터가 존재하지 않음");
    }

    //언어 변환 
    langChange.onclick = function(){ langChangeFunction(); };

    // 단어 선택 이벤틑 리스너 초기화
    initializeEventListeners();

});

// 시간 변환 헬퍼 함수
const parseTime = (timeString) => {
    const parts = timeString.split(':').map(Number);
    return parts.length === 2 ? parts[0] * 60 + parts[1] : 0;
};

const parseAITime = (timeValue) => {
    if (typeof timeValue === 'number') return timeValue;
    if (typeof timeValue === 'string') {
        const parts = timeValue.split(':');
        if (parts.length === 2) return (parseFloat(parts[0]) * 60) + parseFloat(parts[1]);
        return parseFloat(timeValue);
    }
    return 0;
};

const extractVideoId = (url) => {
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([^"&?\/\s]{11})/i;
    const match = url.match(regex);
    return match ? match[1] : null;
};

// YouTube API 준비 콜백
function onYouTubeIframeAPIReady() {
    if (card && card.videoId) {
        createPlayer(card.videoId);
    }
}

const createPlayer = (videoId) => {
    if (player) player.destroy();
    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: videoId,
        playerVars: { 'playsinline': 1, 'controls': 1, 'rel': 0 },
        events: { 'onStateChange': onPlayerStateChange }
    });
};

const onPlayerStateChange = (event) => {
    if (event.data === YT.PlayerState.PLAYING) {
        if (subtitleInterval) clearInterval(subtitleInterval);
        subtitleInterval = setInterval(() => {
            if (!player || typeof player.getCurrentTime !== 'function') {
                clearInterval(subtitleInterval);
                return;
            }
            const currentTime = player.getCurrentTime();
            const relativeTime = currentTime - currentSectionStart;
            const lastIndex = currentTranscript.length - 1;
            const lastTime = lastIndex >= 0 ? parseAITime(currentTranscript[lastIndex].time) : 0;

            let newIndex = -1;
            for (let i = 0; i < currentTranscript.length; i++) {
                const itemTime = parseAITime(currentTranscript[i].time);
                const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;

                if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                    newIndex = i;
                    break;
                }
            }

            if (newIndex === -1 && currentTranscript.length > 0 && relativeTime >= lastTime) {
                newIndex = lastIndex;
            }

            if (newIndex !== currentTranscriptIndex && newIndex !== -1) {
                currentTranscriptIndex = newIndex;
                updateSingleTranscriptLine();
            }

            if (currentTranscriptIndex === lastIndex && relativeTime >= lastTime) {
                player.pauseVideo(); // 마지막 자막 도달 시 영상 일시정지
                clearInterval(subtitleInterval);
                return;
            }

            if (currentTime > currentSectionStart + lastTime + 0.5) {
                clearInterval(subtitleInterval);
                return;
            }
        }, 100);
    } else if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
};

// UI 렌더링 함수
const updateSingleTranscriptLine = () => {
    const transcriptContainer = document.getElementById('transcript-container');
    const japaneseLineElement = document.getElementById('japanese-line');
    const actionablePOSTypes = ['名詞', '動詞', '形容詞'];
    transcriptContainer.classList.toggle('hidden', currentTranscript.length === 0);
    japaneseLineElement.innerHTML = '';

    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];

        if (item.japaneseTokens && Array.isArray(item.japaneseTokens)) {
            item.japaneseTokens.forEach((token, index) => {
                const span = document.createElement('span');
                span.textContent = token.surface;
                span.className = 'japanese-token';
                if(actionablePOSTypes.includes(token.pos)) {
                    span.classList.add('actionable');
                } else {
                    span.classList.add('non-actionable');
                }
                span.dataset.index = index;
                span.tokenData = token;
                japaneseLineElement.appendChild(span);
            });
        } else {
            japaneseLineElement.textContent = item.japanese;
        }

        document.getElementById('korean-line').textContent = item.korean;
        document.getElementById('transcript-index').textContent = `${currentTranscriptIndex + 1} / ${currentTranscript.length}`;
    }
};

// 자막 인덱스 관련 (prev/next)
const changeTranscriptIndex = (direction) => {
    if (currentTranscript.length === 0) return;

    if (direction === -1) {
        if (currentTranscriptIndex === 0) return;
        currentTranscriptIndex -= 1;
    } else if (direction === 1) {
        if (currentTranscriptIndex === currentTranscript.length - 1) return;
        currentTranscriptIndex += 1;
    } else {
        return;
    }

    updateSingleTranscriptLine();

    if (player && typeof player.seekTo === 'function') {
        const relativeTime = parseAITime(currentTranscript[currentTranscriptIndex].time);
        const absoluteTime = currentSectionStart + relativeTime;
        player.seekTo(absoluteTime, true);
        player.playVideo();
    }
};

const renderTimelines = () => {
    const timelineContainer = document.getElementById('timeline-buttons-container');
    timelineContainer.innerHTML = '';

    timelines.forEach((timeline, index) => {
        const button = document.createElement('button');
        button.onclick = () => startTimelinePlayback(timeline);
        timelineContainer.appendChild(button);
    });
};

function renderSectionButtons(sections) {
    const container = document.getElementById('timeline-buttons-container');
    container.innerHTML = '';

    sections.forEach(section => {
        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.textContent = section.section_num || section.sectionNum || "구간";
        button.setAttribute('data-section-id', section.id);

        button.addEventListener('click', () => {
            console.log(`🎯 섹션 버튼 클릭 - ID: ${section.id}, 번호: ${section.section_num || section.sectionNum}`);

            currentSectionStart = section.start_seconds || 0;
            currentTranscript = section.sentences || [];
            currentTranscriptIndex = 0;
            updateSingleTranscriptLine();

            startTimelinePlayback({
                start: currentSectionStart,
                end: section.end_seconds,
                transcript: currentTranscript,
            });
        });

        container.appendChild(button);
    });
}

const startTimelinePlayback = (timeline) => {
    console.log("▶ startTimelinePlayback 실행, timeline 데이터:", timeline);
    if (player) {
        player.seekTo(timeline.start, true);
        player.playVideo();

        clearInterval(timelineTimeout);
        clearInterval(subtitleInterval);

        timelineTimeout = setInterval(() => {
            if (player && typeof player.getCurrentTime === 'function') {
                if (player.getCurrentTime() >= timeline.end) {
                    player.pauseVideo();
                    clearInterval(timelineTimeout);
                }
            }
        }, 100);

        currentTranscript = timeline.transcript;
        currentTranscriptIndex = 0;
        updateSingleTranscriptLine();

        subtitleInterval = setInterval(() => {
            if (!player || typeof player.getCurrentTime !== 'function') {
                clearInterval(subtitleInterval);
                return;
            }
            const currentTime = player.getCurrentTime();
            const relativeTime = currentTime - timeline.start;
            const lastIndex = currentTranscript.length - 1;
            const lastTime = lastIndex >= 0 ? parseAITime(currentTranscript[lastIndex].time) : 0;

            let newIndex = -1;
            for (let i = 0; i < currentTranscript.length; i++) {
                const itemTime = parseAITime(currentTranscript[i].time);
                const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;

                if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                    newIndex = i;
                    break;
                }
            }

            if (newIndex === -1 && currentTranscript.length > 0 && relativeTime >= lastTime) {
                newIndex = lastIndex;
            }

            if (newIndex !== currentTranscriptIndex && newIndex !== -1) {
                currentTranscriptIndex = newIndex;
                updateSingleTranscriptLine();
            }

            if (currentTranscriptIndex === lastIndex && relativeTime >= lastTime) {
                player.pauseVideo(); // 마지막 자막 도달 시 영상 일시정지
                clearInterval(subtitleInterval);
                return;
            }

            if (currentTime > timeline.end + 0.5) {
                clearInterval(subtitleInterval);
                return;
            }
        }, 100);
    }
};


// 말하기 기능 이벤트 리스너
startButton.addEventListener('click', async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });

        audioChunks = []; // 녹음 시작 시 배열 초기화

        mediaRecorder.ondataavailable = event => {
            audioChunks.push(event.data);
        };

        mediaRecorder.onstop = async () => {
            const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
            console.log(audioBlob.size);
            // 서버로 음성 파일 전송
            const formData = new FormData();
            formData.append('audioFile', audioBlob, 'audio.wav'); // 파일 이름과 타입 지정
            formData.append('referenceText', '最近ついてないわって顔をしているそこのあなた。'); // 평가할 문장

            try {
                const response = await fetch('http://localhost:5001/api/speech', {
                    method: 'POST',
                    body: formData,
                });

                if (response.ok) {
                    const result = await response.json();
                    console.log('Pronunciation Assessment Result:', result);
                    alert(`정확도: ${result.accuracyScore}, 유창성: ${result.fluencyScore}`);
                } else {
                    console.error('Server error:', response.statusText);
                }
            } catch (error) {
                console.error('Network error:', error);
            }
        };

        mediaRecorder.start();
        startButton.disabled = true;
        stopButton.disabled = false;
        console.log("녹음 시작");

    } catch (err) {
        console.error("마이크 접근에 실패했습니다:", err);
    }
});

stopButton.addEventListener('click', () => {
    mediaRecorder.stop();
    startButton.disabled = false;
    stopButton.disabled = true;
    console.log("녹음 중지");
});

// 언어 변환 함수
function langChangeFunction(){
    let langChange = document.getElementById('langChange');
    let jp = document.querySelector('.jp')
    let kr = document.querySelector('.kr');
    let now;  // 0: 둘 다, 1: 일본어만, 2: 한국어만

    if(!kr.classList.contains('hidden') && !jp.classList.contains('hidden')){
        now = 0;
    }else if(kr.classList.contains('hidden')){
        now = 1;
    } else {
        now = 2;
    }

    console.log(now + ':now' + ' change Language');
    // console.log(jp.innerHTML + ': jp ' + kr.innerHTML + ' : kr');

    if(now == 0){
        kr.classList.add('hidden');
        now = 1;
        langChange.innerHTML = ' 한 / <strong>일 </strong>';
    }else if(now == 1){
        jp.classList.add('hidden');
        kr.classList.remove('hidden');
        now = 2;
        langChange.innerHTML = '<strong> 한</strong> / 일 ';
    } else { // now == 2;
        jp.classList.remove('hidden');
        now = 0;
        langChange.innerHTML = '<strong> 한 + 일 </strong>';
    }

}

function initializeEventListeners() {

    const japaneseLine = document.getElementById('japanese-line');

    // 토큰 위로 마우스가 올라왔을 때 (Hover)
    japaneseLine.addEventListener('mouseover', handleTokenMouseOver);
    // 토큰에서 마우스가 벗어났을 때
    japaneseLine.addEventListener('mouseout', handleTokenMouseOut);

    // 드래그 시작
    japaneseLine.addEventListener('mousedown', handleSelectionStart);
    // 드래그 중
    japaneseLine.addEventListener('mousemove', handleSelectionMove);
    // 드래그 종료 (문서 전체에서 감지하여 놓치는 경우 방지)
    document.addEventListener('mouseup', handleSelectionEnd);

}
/**
 * 토큰 위로 마우스가 올라왔을 때 하이라이트 및 정보 로깅
 */
function handleTokenMouseOver(event) {
    const target = event.target.closest('.japanese-token');
    if (target && !isSelecting && target.classList.contains('actionable')) { // 드래그 중이 아닐 때만
        target.classList.add('token-hover');
        console.log("마우스 오버:", target.tokenData);
    }
}

/**
 * 토큰에서 마우스가 벗어났을 때 하이라이트 제거
 */
function handleTokenMouseOut(event) {
    const target = event.target.closest('.japanese-token');
    if (target) {
        target.classList.remove('token-hover');
    }
}


// --- 단어 선택 및 팝업 로직 (수정됨) ---

/**
 * (Mousedown) 토큰 위에서 마우스 누르기 시작
 */
function handleSelectionStart(event) {
    // 팝업이 떠 있는 상태라면 무시
    if (isWordSelectorActive) return;

    const target = event.target.closest('.japanese-token');
    if (target) {
        event.preventDefault(); // 텍스트가 파랗게 선택되는 기본 동작 방지
        isSelecting = true;
        selectionStartTokenIndex = parseInt(target.dataset.index, 10);
        selectionEndTokenIndex = selectionStartTokenIndex;
        updateTokenSelectionUI();
    }
}

/**
 * (Mousemove) 토큰 위에서 마우스 드래그
 */
function handleSelectionMove(event) {
    if (isSelecting) {
        const target = event.target.closest('.japanese-token');
        if (target) {
            selectionEndTokenIndex = parseInt(target.dataset.index, 10);
            updateTokenSelectionUI();
        }
    }
}


/**
 * (Mouseup) 팝업을 즉시 표시하고, 백그라운드에서 번역을 요청
 */
function handleSelectionEnd(event) {
    if (isSelecting) {
        isSelecting = false; // isSelecting 상태를 먼저 false로 변경

        const selectedSpans = document.querySelectorAll('.selected-token');
        if (selectedSpans.length > 0) {
            const selectedTokens = Array.from(selectedSpans).map(span => span.tokenData);

            // 1. 팝업을 '번역 중...' 상태로 즉시 표시
            showWordSelector(selectedTokens, null, event);

            // 2. 백그라운드에서 번역 API를 호출하고, 완료되면 팝업 내용을 업데이트
            fetchAndUpdateTranslations(selectedTokens);
        }
    }
}

/**
 * 선택된 토큰들의 배경색을 변경하는 UI 업데이트 함수
 */
function updateTokenSelectionUI() {
    const tokens = document.querySelectorAll('.japanese-token');

    if (selectionStartTokenIndex === -1) {
        tokens.forEach(token => token.classList.remove('selected-token'));
        return;
    }
    const start = Math.min(selectionStartTokenIndex, selectionEndTokenIndex);
    const end = Math.max(selectionStartTokenIndex, selectionEndTokenIndex);

    tokens.forEach((token, index) => {
        // 현재 토큰이 선택 범위에 있고, 'actionable' 클래스를 가지고 있는지 여부를 판단
        const shouldBeSelected = index >= start && index <= end && token.classList.contains('actionable');

        // toggle의 두 번째 인자를 사용하여 클래스를 명시적으로 추가하거나 제거
        token.classList.toggle('selected-token', shouldBeSelected);
    });
}

/**
 * 이미 표시된 팝업의 '뜻' 부분만 업데이트하는 함수
 * @param {Array<string>} translatedWordList - 번역된 뜻 목록
 */
function updatePopupTranslations(translatedWordList) {
    const wordSelector = $('.wordSelector');
    if (!wordSelector.is(':visible')) return; // 팝업이 이미 닫혔으면 중단

    translatedWordList.forEach((meaning, index) => {
        // 각 행의 '뜻' 셀을 찾아 내용을 업데이트
        wordSelector.find(`tr[data-token-index="${index}"] .translation-cell`).text(meaning);
    });
}

/**
 * 단어 선택 팝업을 표시하는 함수 (번역된 단어 리스트를 파라미터로 받음)
 * @param {Array} selectedTokens - 선택된 토큰 객체 배열
 * @param {Array<string>} translatedWordList - 번역된 뜻 목록
 * @param {MouseEvent} event - 마우스 이벤트 객체
 */
function showWordSelector(selectedTokens, translatedWordList, event) {
    const wordSelector = $('.wordSelector');
    if (!selectedTokens || selectedTokens.length === 0) return;

    clearGlobalClickListener();

    const combinedText = selectedTokens.map(token => token.surface).join('');

    const detailsTableHTML = `
        <table class="token-details-table">
            <thead>
                <tr>
                    <th>품사</th>
                    <th>단어</th>
                    <th>원형</th>
                    <th>뜻</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                ${selectedTokens.map((token, index) => {
        // 번역 리스트가 없으면 '번역 중...' 텍스트 표시
        const meaningText = translatedWordList ? (translatedWordList[index] || '') : '번역 중...';
        return `
                        <tr data-token-index="${index}">
                            <td>${token.pos || ''}</td>
                            <td>${token.surface || ''}</td>
                            <td>${token.base || ''}</td>
                            <td class="translation-cell">${meaningText}</td>
                            <td>
                                <button class="select-wordbook-btn" data-token='${JSON.stringify(token)}'>+</button>
                            </td>
                        </tr>
                    `;
    }).join('')}
            </tbody>
        </table>
    `;
    const popupHTML = `
        <div class="selected-words-container">
            <!--<div class="combined-word-display">${combinedText}</div>-->
            <hr>
            <div class="token-details-container">${detailsTableHTML}</div>
        </div>
    `;

    wordSelector.html(popupHTML).addClass('on').show();
    isWordSelectorActive = true;

    const selectedSpans = document.querySelectorAll('.selected-token');
    let minLeft = Infinity, maxRight = -Infinity, top = Infinity;
    selectedSpans.forEach(span => {
        const rect = span.getBoundingClientRect();
        if (rect.left < minLeft) minLeft = rect.left;
        if (rect.right > maxRight) maxRight = rect.right;
        if (rect.top < top) top = rect.top;
    });

    const combinedWidth = maxRight - minLeft;
    const posX = minLeft + window.scrollX + (combinedWidth / 2);
    const selectionTop = top + window.scrollY;
    const popupHeight = wordSelector.outerHeight();
    const margin = 10;
    const finalPopupTop = selectionTop - popupHeight - margin;

    wordSelector.css({
        left: (posX - (wordSelector.outerWidth() / 2)) + 'px',
        top: finalPopupTop + 'px'
    });

    wordSelector.off('click', '.select-wordbook-btn').on('click', '.select-wordbook-btn', function() {
        event.stopPropagation();
        const tokenData = $(this).data('token');
        const meaning = document.querySelector('.translation-cell').textContent;
        displayWordbookList(tokenData, meaning);
    });

    setTimeout(() => { if (isWordSelectorActive) addGlobalClickListener(); }, 300);
}

/**
 * 단어장 목록 선택 UI를 팝업에 표시하는 함수
 * @param {object} tokenToSave - 저장할 대상 토큰 정보
 */
async function displayWordbookList(tokenToSave, meaning) {
    const wordbookSelector = $('.wordbookSelector');
    wordbookSelector.html('<div>단어장 목록 로딩 중...</div>').show();

    // 단어장 팝업 위치 계산
    const rect = $('.wordSelector').get(0).getBoundingClientRect();
    wordbookSelector.css({
        left: (rect.right + 10) + 'px',
        top: (rect.top + window.scrollY) + 'px',
    });

    try {
        let books = [];
        await fetch(`/mochilearn/api/wordbook/list`)
        .then(response => response.json())
        .then(data => {
            console.log('단어장 받음');

            books = data.data;
            console.log(books);
        });

        const wordbookListHTML = books.map(book => `
            <div class="wordbook-item">
                <input type="radio" name="wordbook" value="${book.id}" id="book-${book.id}">
                <label for="book-${book.id}">${book.title}</label>
            </div>`).join('');

        const selectionHTML = `
            <div class="wordbook-selection-container">
                <h4>'${tokenToSave.surface}' 저장</h4>
                <div class="wordbook-list">${wordbookListHTML}</div>
                <div class="wordbook-actions">
                    <button class="final-save-btn">저장</button>
                </div>
            </div>`;
        wordbookSelector.html(selectionHTML);

        // '단어장에 저장' 버튼 이벤트
        wordbookSelector.find('.final-save-btn').click(async function() {
            const selectedBookId = $('input[name="wordbook"]:checked').val();
            if (!selectedBookId) {
                alert('저장할 단어장을 선택해주세요.');
                return;
            }
            console.log(`단어장 ID: ${selectedBookId}에 토큰 저장:`, tokenToSave);


            const word = tokenToSave.surface || tokenToSave.base;
            const data = {
                "word": word,
                "meaning": meaning,
                "pos": tokenToSave.pos,
                "bookId": selectedBookId
            };
            await fetch('/mochilearn/api/word/save', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            })
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                })
                .then(data => {
                    console.log('단어 저장 성공', data);
                    alert('단어를 저장했습니다.');
                })
                .catch(error => {
                    console.error('Error', error);
                    alert('단어 저장에 실패했습니다.');
                });

            hidePopups();
        });
    } catch (error) {
        wordbookSelector.html('<div>목록 로딩 실패</div>');
    }
}

/**
 * 팝업 외부 클릭 시 팝업을 닫기 위한 전역 리스너 추가
 */
function addGlobalClickListener() {
    if (globalClickListener) return;
    globalClickListener = (e) => {
        if (!isWordSelectorActive) return;

        const wordSelector = document.querySelector('.wordSelector');
        const wordbookSelector = document.querySelector('.wordbookSelector');

        const isClickInWordPopup = wordSelector.contains(e.target);
        const isClickInWordbookPopup = wordbookSelector.contains(e.target);

        if (!isClickInWordPopup && !isClickInWordbookPopup) {
            hidePopups();
        }
    };
    document.addEventListener('click', globalClickListener);
}

function clearGlobalClickListener() {
    if (globalClickListener) {
        document.removeEventListener('click', globalClickListener);
        globalClickListener = null;
    }
}

/**
 * 전역 클릭 리스너 제거
 */
function clearGlobalClickListener() {
    if (globalClickListener) {
        document.removeEventListener('click', globalClickListener);
        globalClickListener = null;
    }
}

/**
 * 단어 선택 팝업을 숨기고 선택 상태를 초기화하는 함수
 */
function hidePopups() {
    $('.wordSelector').removeClass('on').hide();
    $('.wordbookSelector').hide(); // 단어장 팝업도 숨김
    isWordSelectorActive = false;
    selectionStartTokenIndex = -1;
    selectionEndTokenIndex = -1;
    updateTokenSelectionUI();
    clearGlobalClickListener();
}

/**
 * 서버에 단어 번역을 요청하고, 이미 열려있는 팝업의 내용을 업데이트하는 함수
 * @param {Array} selectedTokens - 선택된 토큰 객체 배열
 */
async function fetchAndUpdateTranslations(selectedTokens) {
    const wordList = selectedTokens.map(token => token.base || token.surface);
    const context = currentTranscript[currentTranscriptIndex].japanese;

    const requestData = { context, wordList };

    try {
        const response = await fetch(`/mochilearn/api/word/translate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) throw new Error('번역 API 응답 실패');

        const result = await response.json();
        const translatedWordList = result.data;

        // 팝업의 '뜻' 부분을 번역된 내용으로 업데이트
        updatePopupTranslations(translatedWordList);

    } catch (error) {
        console.error("번역 중 오류 발생:", error);
        const errorMessages = selectedTokens.map(() => '번역 실패');
        updatePopupTranslations(errorMessages);
    }
}