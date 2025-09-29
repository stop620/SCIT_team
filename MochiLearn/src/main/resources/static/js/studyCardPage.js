let globalClickListener = null;
let isWordSelectorActive = false;

document.addEventListener('DOMContentLoaded', ()=>{
    const urlInput = document.getElementById('url-input');
    const favBtn = document.getElementById('favBtn');
    const langChange = document.getElementById('langChange');
    const jp = document.querySelector('.jp');

    urlInput.addEventListener('change', (event) => {
        const videoId = extractVideoId(event.target.value);
        if (videoId) {
            createPlayer(videoId);
        }
    });

    jp.addEventListener('selectstart',()=>{
        jp.addEventListener('mouseup',(event)=>{
            const wordSel = window.getSelection().toString();
            textVal(wordSel,event);
        });
        jp.addEventListener('mousemove',(e)=>{
            //console.log(e.clientX,e.clientY);
        });
    });

    favBtn.onclick = function(){ fav(); };
    langChange.onclick = function(){ langChangeFunction(); };

})

const createPlayer = (videoId) => {
    player = new YT.Player('player', {
        height: '300px',
        width: '300px',
        videoId: videoId,
        playerVars: { 'playsinline': 1, 'controls': 1, 'rel': 0 },
        events: { 'onStateChange': onPlayerStateChange }
    });
};

const extractVideoId = (url) => {
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i;
    const match = url.match(regex);
    return match ? match[1] : null;
};

const onPlayerStateChange = (event) => {
    // 영상이 정지되거나 끝나면, 모든 자동화 타이머(자막, 구간정지)를 중단합니다.
    if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
};

function fav() {

    // 백에서 프런트로 데이터를 뿌려준 후
    // 해당 데이터를 끌고와서 계산합니다.
    let favNumElem = document.getElementById('favNum');
    let favBtnElem = document.getElementById('favBtn');
    let favNum = parseInt(favNumElem.innerText, 10);

    // 버튼이 비활성(☆)이면 +1, 활성(★)이면 -1
    if (favBtnElem.value === "☆") {
        favNum += 1;
        favBtnElem.value = "★";
    } else {
        favNum -= 1;
        favBtnElem.value = "☆";
    }
    favNumElem.innerText = favNum.toString();

    console.log(favNum + " : favNum");
}

function langChangeFunction(){
    let langChange = document.getElementById('langChange');
    let jp = document.querySelector('.jp')
    let kr = document.querySelector('.kr');
    var now;

    if(kr.classList.contains('hidden')){
        now = 0; 
    }else{
        now = 1;
    }
	console.log(now + ':now' + ' change Language');
    // console.log(jp.innerHTML + ': jp ' + kr.innerHTML + ' : kr');

    if(now == 1){
        jp.classList.remove('hidden');
        kr.classList.add('hidden');
        now = 0;
        langChange.innerHTML = '한 / <strong>일</strong>';
    }else if(now == 0){
        jp.classList.add('hidden');
        kr.classList.remove('hidden');
        now = 1;
        langChange.innerHTML = '<strong>한</strong> / 일';
    }

}

function textVal(wordSel, event) {
    const word = document.querySelector('.wordSelector');
    
    if (wordSel.length > 0) {
        // 이전 상태 정리
        clearGlobalClickListener();
        
		//단어 선택 후 저장하기 버튼
        word.innerHTML = wordSel + 
		'<div><button class="saveBtn">저장하기</button></div>';
        
		word.classList.add('on');
        word.style.display = 'block';
        isWordSelectorActive = true;
        
        // Selection 객체를 사용하여 선택된 텍스트의 정확한 위치 가져오기
        const selection = window.getSelection();
        if (selection.rangeCount > 0) {
            const range = selection.getRangeAt(0);
            const rect = range.getBoundingClientRect();
            
            // 선택된 텍스트의 중앙 위치 계산
            const posX = rect.left + window.scrollX + (rect.width / 2);
            const posY = rect.top + window.scrollY - 40; // wordSelector가 위에 나타나도록
            
            // wordSelector를 중앙 정렬하기 위해 자신의 너비의 절반만큼 왼쪽으로 이동
            word.style.left = (posX - word.offsetWidth / 2) + 'px';
            word.style.top = (posY - 25) + 'px';
            
            console.log('selector position = ' + posX + ' : ' + posY);
        }
        
        // 충분한 지연 후 전역 클릭 이벤트 추가
        setTimeout(() => {
            if (isWordSelectorActive) {
                addGlobalClickListener();
            }
        }, 300);
        
    } else {
        removeEvent();
    }
}

function addGlobalClickListener() {
    if (globalClickListener) return; // 이미 있으면 추가하지 않음
    
    globalClickListener = function(e) {
        const word = document.querySelector('.wordSelector');
        // wordSelector 영역이 아닌 곳을 클릭했을 때만 removeEvent 실행
        if (isWordSelectorActive && !word.contains(e.target)) {
            removeEvent();
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

function removeEvent() {
    const word = document.querySelector('.wordSelector');
    
    word.classList.remove('on');
    word.style.display = 'none';
    isWordSelectorActive = false;
    
    // 선택 해제
    if (window.getSelection) {
        window.getSelection().removeAllRanges();
    }
    
    // 클릭 이벤트 제거
    clearGlobalClickListener();
}

// DOMContentLoaded에서 초기화
document.addEventListener('DOMContentLoaded', () => {
    // 기존 코드...
    
    const jp = document.querySelector('.jp');
    let isSelecting = false;

    jp.addEventListener('mousedown', () => {
        isSelecting = true;
    });

    jp.addEventListener('mouseup', (event) => {
        if (isSelecting) {
            // 약간의 지연을 주어 selection이 완료되도록 함
            setTimeout(() => {
                const wordSel = window.getSelection().toString().trim();
                if (wordSel.length > 0) {
                    textVal(wordSel, event);
                }
                isSelecting = false;
            }, 50);
        }
    });

    // 페이지 로드시 wordSelector 숨김
    const word = document.querySelector('.wordSelector');
    if (word) {
        word.style.display = 'none';
    }
});