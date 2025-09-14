
// 자막을 UI에 렌더링하고, 이전/다음 버튼 및 언어 전환 기능을 관리

/**
 * 자막 관련 UI 컨트롤(버튼)의 이벤트 리스너를 초기화
 */
function initializeTranscriptControls() {
    $('#prev-btn').click(() => changeTranscriptIndex(-1));
    $('#next-btn').click(() => changeTranscriptIndex(1));
    $('#langChange').click(langChangeFunction);
}

/**
 * 현재 자막을 UI에 표시하거나 업데이트하는 함수
 */
function updateSingleTranscriptLine() {
    const transcriptContainer = $('#transcript-container');
    const japaneseLineElement = $('#japanese-line');

    transcriptContainer.toggleClass('hidden', currentTranscript.length === 0);
    japaneseLineElement.empty();

    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];

        if (item.japaneseTokens && Array.isArray(item.japaneseTokens)) {
            item.japaneseTokens.forEach((token, index) => {
                const span = $('<span></span>', {
                    text: token.surface,
                    class: `japanese-token ${isActionableToken(token) ? 'actionable' : 'non-actionable'}`,
                    'data-index': index
                }).prop('tokenData', token);
                japaneseLineElement.append(span);
            });
        } else {
            japaneseLineElement.text(item.japanese);
        }

        $('#korean-line').text(item.korean);
        $('#transcript-index').text(`${currentTranscriptIndex + 1} / ${currentTranscript.length}`);
    }
}

/**
 * 이전/다음 버튼 클릭 시 자막 인덱스를 변경하고 비디오 시간을 이동시키는 함수
 * @param {number} direction - -1 (이전) 또는 1 (다음)
 */
function changeTranscriptIndex(direction) {
    if (currentTranscript.length === 0) return;

    const newIndex = currentTranscriptIndex + direction;
    if (newIndex >= 0 && newIndex < currentTranscript.length) {
        currentTranscriptIndex = newIndex;
        updateSingleTranscriptLine();
        if (player && typeof player.seekTo === 'function') {
            const relativeTime = parseAITime(currentTranscript[currentTranscriptIndex].time);
            player.seekTo(currentSectionStart + relativeTime, true);
        }
    }
}

/**
 * 자막의 언어 표시를 전환하는 함수 (한/일, 한, 일)
 */
function langChangeFunction() {
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
