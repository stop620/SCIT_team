
$(document).ready(function() {
    // 모달 열기
    $('#tagBtn').click(function() {
        $('#tagModal').css('display', 'flex').hide().fadeIn(200);
    });

    // 모달 닫기 (X 버튼)
    $('.close-btn').click(function() {
        $('#tagModal').fadeOut(200);
    });

    // 모달 바깥 클릭 시 닫기
    $(window).click(function(event) {
        if ($(event.target).is('#tagModal')) {
            $('#tagModal').fadeOut(200);
        }
    });

    // 태그 선택 토글
    $('#tagList').on('click', '.tagSelect', function() {
        $(this).toggleClass('selected');
    });

    // 필터 적용 버튼 클릭 시
    $('#applyTagFilter').click(function() {
        // 선택된 태그 수집
        let selectedTags = [];
        $('.tagSelect.selected').each(function() {
            selectedTags.push($(this).data('value'));
        });
        
        if(selectedTags.length===0){
            alert('태그를 하나이상 선택하세요.')
            return;
        }

        console.log("선택된 태그:", selectedTags);

        // AJAX 호출 예시 (서버 URL에 맞게 수정)
        $.ajax({
            url: '/api/study/filterByTags',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(selectedTags),
            success: function(cards) {
                // 기존 카드 목록 새로 렌더링하는 함수 호출
                renderCards(cards, true);
                $('#tagModal').fadeOut(200);
            },
            error: function() {
                alert('태그 필터링 중 오류가 발생했습니다.');
            }
        });
    });
});
function getYoutubeThumbnail(youtubeUrl) {
    let videoId = '';
    if (!youtubeUrl) {
        return '/images/default-thumbnail.png';  // URL이 없으면 기본 이미지 반환
    }

    if (youtubeUrl.includes('youtu.be/')) {
        videoId = youtubeUrl.split('youtu.be/')[1].split(/[?&]/)[0];
    } else if (youtubeUrl.includes('v=')) {
        videoId = youtubeUrl.split('v=')[1].split(/[?&]/)[0];
    }
    if (videoId) {
        return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
    }

    return '/images/default-thumbnail.png';
}

let page = 0;
const size = 12;
let sort = 'popular';
let loading = false;
let endReached = false;

function renderCards(cards, reset = false) {
    if (reset) {
        $('#cardGrid').empty(); // 정렬 변경 시 기존 목록 제거
    }

    cards.forEach(card => {
        const thumbUrl = getYoutubeThumbnail(card.url);
        const cardHtml = `
            <div class="card" onclick="location.href='/mochilearn/page/studyCard?cardId=${card.id}'">
                <h3>${card.title}</h3>
                <img src="${thumbUrl}" alt="썸네일 이미지" />
            </div>
        `;
        $('#cardGrid').append(cardHtml);
    });
}

let search = "";

function loadCards(reset = false) {
    if (reset) {
        page = 0;
        endReached = false;
    }

    if (loading || endReached) return;

    loading = true;

    $.get('/mochilearn/api/study/load', { 
            sort: sort, 
            page: page, 
            size: size, 
            search: search
        })
        .done(function(data) {
            if ((!data || data.length === 0)&& search.trim() !== '') {
                alert('검색 결과가 없습니다.');
                $('#searchInput').val(''); // 검색창 초기화
                search = '';
                endReached = true;
                return;
            }
            if (reset) $('#cardGrid').empty();
            renderCards(data, reset);
            page++;
        })
        .fail(function(jqXHR, textStatus, errorThrown) {
            console.error("AJAX 요청 실패:", textStatus, errorThrown);
        })
        .always(function() { loading = false; });

}

$(document).ready(function() {
    // 초기 로드
    loadCards(true);

    // 정렬 변경
    $('#sortBox').change(function() {
        console.log("드롭다운 변경 이벤트 발생!");
        sort = $(this).val();
        loadCards(true);
    });

    // 검색 버튼 클릭 시
    $('#searchBtn').on('click', function() {
        search = $('#searchInput').val().trim(); // 검색어 상태 업데이트
        loadCards(true); // 검색할 때는 항상 카드 초기화
        
    });

    // 엔터키로도 검색 가능하게
    $('#searchInput').on('keydown', function(e) {
        if (e.key === 'Enter') {
            search = $(this).val().trim();
            loadCards(true);
        }
    });

    // 스크롤로 다음 페이지 로드
    $(window).scroll(function() {
        if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
            loadCards(false);
        }
    });

    // 버튼 클릭 시 부드럽게 위로 스크롤
    $('#scrollTopBtn').on('click', function() {
        $('html, body').animate({ scrollTop: 0 }, 300); //0.3초동안 스크롤업
    });
});
