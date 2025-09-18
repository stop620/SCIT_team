
// 일본어 자막에서 단어를 선택하고, 번역을 요청하며, 단어장에 저장하는 UI 로직

let isSelecting = false;
let selectionStartTokenIndex = -1;
let selectionEndTokenIndex = -1;
let globalClickListener = null;
let isWordSelectorActive = false;

// 사전 검색 결과 저장해두는 캐시
const dictionaryCache = {};

function initializeWordSelectionEventListeners() {
    const japaneseLine = document.getElementById('japanese-line');
    const langChangeBtn = document.getElementById('langChange');
    if (japaneseLine) {
        japaneseLine.addEventListener('mouseover', handleTokenMouseOver);
        japaneseLine.addEventListener('mouseout', handleTokenMouseOut);
        japaneseLine.addEventListener('mousedown', handleSelectionStart);
        japaneseLine.addEventListener('mousemove', handleSelectionMove);
        document.addEventListener('mouseup', handleSelectionEnd);
    }
    langChangeBtn.addEventListener('click', langChangeFunction);
}

const isActionableToken = (token) => {
    if (!token || !token.pos) return false;
    const actionablePOSTypes = ['名詞', '動詞', '形容詞'];
    return actionablePOSTypes.includes(token.pos);
};

function handleTokenMouseOver(event) {
    const target = event.target.closest('.japanese-token');
    if (target && !isWordSelectorActive && !isSelecting && target.classList.contains('actionable')) {
        target.classList.add('token-hover');
    }
}

function handleTokenMouseOut(event) {
    const target = event.target.closest('.japanese-token');
    if (target) {
        target.classList.remove('token-hover');
    }
}

function handleSelectionStart(event) {
    if (isWordSelectorActive) return;
    const target = event.target.closest('.japanese-token');
    if (target && target.classList.contains('actionable')) {
        event.preventDefault();
        isSelecting = true;
        document.querySelectorAll('.japanese-token.token-hover').forEach(el => el.classList.remove('token-hover'));
        selectionStartTokenIndex = parseInt(target.dataset.index, 10);
        selectionEndTokenIndex = selectionStartTokenIndex;
        updateTokenSelectionUI();
    }
}

function handleSelectionMove(event) {
    if (isSelecting) {
        const target = event.target.closest('.japanese-token');
        if (target && target.classList.contains('actionable')) {
            selectionEndTokenIndex = parseInt(target.dataset.index, 10);
            updateTokenSelectionUI();
        }
    }
}

// 단어 선택 끝
function handleSelectionEnd(event) {
    if (isSelecting) {
        isSelecting = false;
        const selectedSpans = document.querySelectorAll('.selected-token');
        if (selectedSpans.length > 0) {
            const selectedTokens = Array.from(selectedSpans).map(span => span.tokenData);
            showWordSelector(selectedTokens, event);
        }
    }
}

function updateTokenSelectionUI() {
    const tokens = document.querySelectorAll('.japanese-token');
    if (selectionStartTokenIndex === -1) {
        tokens.forEach(token => token.classList.remove('selected-token'));
        return;
    }
    const start = Math.min(selectionStartTokenIndex, selectionEndTokenIndex);
    const end = Math.max(selectionStartTokenIndex, selectionEndTokenIndex);

    tokens.forEach((token, index) => {
        const shouldBeSelected = index >= start && index <= end && token.classList.contains('actionable');
        token.classList.toggle('selected-token', shouldBeSelected);
    });
}

/**
 * 사전 검색 API를 호출하고 결과를 팝업에 렌더링하는 함수
 * @param {Array} selectedTokens - 선택된 토큰 객체 배열
 */
async function fetchDictionaryEntry(tokenToSearch) {

    console.log(tokenToSearch);
    let searchTerm;
    if(tokenToSearch.base === '*') {
        searchTerm = tokenToSearch.surface;
    } else {
        searchTerm = tokenToSearch.base;
    }
    if (!searchTerm) {
        renderDictionaryPopup(null, "검색할 단어를 찾을 수 없습니다.");
        return;
    }

    // 1. 캐시 확인: 이미 검색한 단어인지 확인
    if (dictionaryCache[searchTerm]) {
        console.log(`[Cache Hit] '${searchTerm}'의 검색 결과를 캐시에서 불러옵니다.`);
        renderDictionaryPopup(dictionaryCache[searchTerm]);
        return;
    }

    console.log(`[Cache Miss] '${searchTerm}'의 검색 결과를 API로 요청합니다.`);
    try {
        const response = await fetch(`http://localhost:5001/api/dictionary/search?query=${encodeURIComponent(searchTerm)}`);
        if (!response.ok) throw new Error('사전 검색 API 응답 실패');

        const result = await response.json();
        if (result.success && result.data) {
            // 2. 캐시 저장: API 호출 성공 시 결과를 캐시에 저장
            dictionaryCache[searchTerm] = result.data;
            renderDictionaryPopup(result.data);
        } else {
            throw new Error(result.message || "단어를 찾을 수 없습니다.");
        }
    } catch (error) {
        console.error("사전 검색 중 오류 발생:", error);
        renderDictionaryPopup(null, error.message);
    }
}

/**
 * 팝업 위치를 재계산하고 설정하는 함수
 */
function repositionPopup() {
    const wordSelector = $('.wordSelector');
    if (!wordSelector.is(':visible')) return;

    const selectionRect = wordSelector.data('selectionRect');
    if (selectionRect) {
        const { minLeft, maxRight, top } = selectionRect;
        const combinedWidth = maxRight - minLeft;
        const posX = minLeft + window.scrollX + (combinedWidth / 2);
        const selectionTop = top + window.scrollY;

        // setTimeout을 사용하여 브라우저가 렌더링을 완료한 후 높이를 계산
        setTimeout(() => {
            const popupHeight = wordSelector.outerHeight();
            const finalPopupTop = selectionTop - popupHeight - 10; // 10px margin
            wordSelector.css({
                left: (posX - (wordSelector.outerWidth() / 2)) + 'px',
                top: finalPopupTop + 'px'
            });
        }, 0);
    }
}

/**
 * 팝업 내용을 사전 검색 결과로 채우는 함수
 * @param {object|null} data - API로부터 받은 사전 데이터
 * @param {string|null} errorMessage - 에러 발생 시 표시할 메시지
 */
function renderDictionaryPopup(data, errorMessage = null) {
    const container = $('.wordSelector .dictionary-details-container');
    if (!container.length) return;

    if (errorMessage) {
        container.html(`<div class="error-message">${errorMessage}</div>`);
    } else {
        const kanjiHTML = data.kanji.join(', ') || '표기 없음';
        const kanaHTML = data.kana.join(', ') || '읽기 없음';
        const posHTML = data.partOfSpeech.join(', ') || '품사 없음';
        const meaningText = data.gloss.join(', ') || '뜻 정보 없음'; // 뜻은 하나만 표시
        const exampleHTML = data.examples // 예문도 하나만 표시
            ? `<p class="jp-example">${data.examples[0] || ''}</p>
               <p class="kr-example">${data.examples[1] || ''}</p>`
            : '예문 정보 없음';


        const originalWord = $('.wordSelector').find('.word-display').text();
        const saveWord = {selectWord: originalWord, ...data};


        const finalHTML = `
            <div class="dict-entry-grid">
                <div class="dict-label">표기</div><div class="dict-content">${kanjiHTML}</div>
                <div class="dict-label">읽기</div><div class="dict-content">${kanaHTML}</div>
                <div class="dict-label">품사</div><div class="dict-content">${posHTML}</div>
                <div class="dict-label">뜻</div><div class="dict-content">${meaningText}</div>
                <div class="dict-label">예문</div><div class="dict-content">${exampleHTML}</div>
            </div>
            <div class="popup-actions">
                <button class="save-word-btn" data-word='${JSON.stringify(saveWord)}'>단어장에 저장</button>
            </div>`;
        container.html(finalHTML);
    }
    // 내용이 채워진 후 최종 위치 계산
    repositionPopup();
}

/**
 * 단어 전환 UI를 업데이트하는 함수
 */
function updateCarouselView() {
    const wordSelector = $('.wordSelector');
    const tokens = wordSelector.data('selectedTokens');
    const currentIndex = wordSelector.data('currentDisplayIndex');
    const currentToken = tokens[currentIndex];

    const displayWord = currentToken.base && currentToken.base !== '*' ? currentToken.base : currentToken.surface;
    wordSelector.find('.word-display').text(displayWord);
    wordSelector.find('.popup-counter').text(`${currentIndex + 1} / ${tokens.length}`);

    // 캐시 확인 로직
    const searchTerm = currentToken.base && currentToken.base !== '*' ? currentToken.base : currentToken.surface;
    if (dictionaryCache[searchTerm]) {
        console.log(`[Cache Hit] '${searchTerm}'의 검색 결과를 캐시에서 즉시 렌더링합니다.`);
        renderDictionaryPopup(dictionaryCache[searchTerm]);
    } else {
        wordSelector.find('.dictionary-details-container').html('<div>사전 검색 중...</div>');
        repositionPopup();
        fetchDictionaryEntry(currentToken);
    }
}

/**
 * 단어 선택 팝업 (기본 틀과 로딩 상태만 표시)
 */
function showWordSelector(selectedTokens, event) {
    const wordSelector = $('.wordSelector');
    if (!selectedTokens || selectedTokens.length === 0) return;

    clearGlobalClickListener();
    const actionableTokens = selectedTokens.filter(isActionableToken);
    if (actionableTokens.length === 0) return;

    wordSelector.data('selectedTokens', actionableTokens);
    wordSelector.data('currentDisplayIndex', 0);

    const isMultiple = actionableTokens.length > 1;
    const initialToken = actionableTokens[0];
    const initialWord = initialToken.base && initialToken.base !== '*' ? initialToken.base : initialToken.surface;

    const popupHTML = `
        <div class="selected-words-container">
            <div class="word-carousel-header">
                <button class="popup-nav-arrow left ${isMultiple ? '' : 'hidden'}">⬅️</button>
                <div class="word-display">${initialWord}</div>
                <button class="popup-nav-arrow right ${isMultiple ? '' : 'hidden'}">➡️</button>
            </div><hr>
            <div class="dictionary-details-container"></div>
            <div class="popup-counter ${isMultiple ? '' : 'hidden'}"></div>
        </div>`;

    wordSelector.html(popupHTML).addClass('on').show();
    isWordSelectorActive = true;

    // 선택 영역의 좌표를 저장
    const selectedSpans = document.querySelectorAll('.selected-token');
    let minLeft = Infinity, maxRight = -Infinity, top = Infinity;
    selectedSpans.forEach(span => {
        const rect = span.getBoundingClientRect();
        if (rect.left < minLeft) minLeft = rect.left;
        if (rect.right > maxRight) maxRight = rect.right;
        if (rect.top < top) top = rect.top;
    });
    wordSelector.data('selectionRect', { minLeft, maxRight, top });

    // 팝업을 보이지 않는 곳에 임시로 둠
    wordSelector.css({ left: '-9999px', top: '-9999px' });

    updateCarouselView(); // 초기 뷰 렌더링 및 첫 단어 검색 시작

    // 이벤트 리스너 설정
    wordSelector.off('click').on('click', '.save-word-btn', function() {
        displayWordbookList($(this).data('word'));
    }).on('click', '.popup-nav-arrow.left', function() {
        let index = wordSelector.data('currentDisplayIndex');
        index = (index - 1 + actionableTokens.length) % actionableTokens.length;
        wordSelector.data('currentDisplayIndex', index);
        updateCarouselView();
    }).on('click', '.popup-nav-arrow.right', function() {
        let index = wordSelector.data('currentDisplayIndex');
        index = (index + 1) % actionableTokens.length;
        wordSelector.data('currentDisplayIndex', index);
        updateCarouselView();
    });

    setTimeout(() => { if (isWordSelectorActive) addGlobalClickListener(); }, 300);
}

/**
 * 단어장 목록 선택 UI를 팝업에 표시하는 함수
 * @param {object} wordToSave - 저장할 대상 토큰 정보
 */
async function displayWordbookList(wordToSave) {
    console.log("저장할 단어: ", wordToSave);

    const wordbookSelector = $('.wordbookSelector');
    wordbookSelector.html('<div>단어장 목록 로딩 중...</div>').show();

    const wordSelectorRect = $('.wordSelector').get(0).getBoundingClientRect();
    wordbookSelector.css({
        left: (wordSelectorRect.right + 10) + 'px',
        top: (wordSelectorRect.top + window.scrollY) + 'px', // 스크롤 위치 보정
        height: 'auto'
    });

    try {
        const response = await fetch('/mochilearn/api/wordbook/list');
        const result = await response.json();
        const books = result.data || [];

        const wordbookListHTML = books.map(book => `
            <div class="wordbook-item">
                <input type="radio" name="wordbook" value="${book.id}" id="book-${book.id}">
                <label for="book-${book.id}">${book.title}</label>
            </div>`).join('');

        const selectionHTML = `
            <div class="wordbook-selection-container">
                <h4>'${wordToSave.selectWord}' 저장</h4>
                <div class="wordbook-list">${wordbookListHTML}</div>
                <div class="wordbook-actions"><button class="final-save-btn">저장</button></div>
            </div>`;
        wordbookSelector.html(selectionHTML);

        wordbookSelector.find('.final-save-btn').click(async function() {
            const selectedBookId = $('input[name="wordbook"]:checked').val();
            if (!selectedBookId) {
                alert('저장할 단어장을 선택해주세요.');
                return;
            }
            const dataToSave = { ...wordToSave, bookId: selectedBookId };
            try {
                const saveResponse = await fetch('/mochilearn/api/word/save', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(dataToSave)
                });
                const saveResult = await saveResponse.json();
                if (saveResult.success) {
                    alert('단어를 저장했습니다.');
                } else {
                    alert(saveResult.message || '이미 저장된 단어입니다.');
                }
            } catch (err) {
                alert('단어 저장에 실패했습니다.');
            } finally {
                hidePopups();
            }
        });
    } catch (error) {
        wordbookSelector.html('<div>목록 로딩 실패</div>');
    }
}

function addGlobalClickListener() {
    if (globalClickListener) return;
    globalClickListener = (e) => {
        if (!isWordSelectorActive) return;
        const wordSelector = document.querySelector('.wordSelector');
        const wordbookSelector = document.querySelector('.wordbookSelector');
        if (!wordSelector.contains(e.target) && !wordbookSelector.contains(e.target)) {
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

function hidePopups() {
    $('.wordSelector').removeClass('on').hide();
    $('.wordbookSelector').hide();
    isWordSelectorActive = false;
    selectionStartTokenIndex = -1;
    selectionEndTokenIndex = -1;
    updateTokenSelectionUI();
    clearGlobalClickListener();
}

// 한/일 자막 전환 함수
function langChangeFunction() {
    console.log('change');
    const jp = $('#japanese-line');
    const kr = $('#korean-line');
    const langChange = $('#langChange');

    if (jp.is(':visible') && kr.is(':visible')) {
        kr.hide();
        langChange.html('한 / <strong>일</strong>');
    } else if (jp.is(':visible')) {
        jp.hide();
        kr.show();
        langChange.html('<strong>한</strong> / 일');
    } else {
        jp.show();
        langChange.html('<strong>한 + 일</strong>');
    }
}