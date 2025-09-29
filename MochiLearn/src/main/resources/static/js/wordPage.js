document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM 로드')
    let books = [];

    const wordBookList = document.querySelector('.wordBookList');

    // 단어장 목록 로딩
    function loadBooks() {
        console.log('단어장 요청');
        fetch(`/mochilearn/api/wordbook/list`)
            .then(response => response.json())
            .then(data => {
                console.log('단어장 받음');

                books = data.data;
                console.log(books);

                renderBooks();
                initializeListEventListener();
            })
            .finally( data => {
                if(data != null){
                }
            });
    }


    // 단어장 렌더링
    function renderBooks() {

        let bookHtml = '';

        /*bookHtml += `<div class="addWordCard">
                <h3>단어장 목록</h3>
                <a href="/mochilearn/page/addWordCard">단어장 추가</a>
            </div>
            <ul>`;*/

        // books.forEach((book,index) => {
        //     bookHtml += `
        //         <div class="wordList">
        //         <div class="wordCard">
        //             <a href="/mochilearn/page/wordCard?bookId=${book.id}">${book.title}</a>
        //         </div>
        //     </div>
        //     `;
        // });

        books.forEach((book,index) => {
            bookHtml += `
            <li data-book-id="${book.id}">
                <div class="wordCardList">
                    <a href="/mochilearn/page/wordCard?bookId=${book.id}">${book.title}</a>
                    <p class="wordCount">${book.wordCount}단어</p>
                    <svg class="deleteBtn" fill="#757575" xmlns="http://www.w3.org/2000/svg" width="1rem" height="1rem" viewBox="0 0 123.7 123.7" xml:space="preserve" stroke="#757575"><g id="SVGRepo_bgCarrier" stroke-width="0"></g><g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g><g id="SVGRepo_iconCarrier"> <g> <path d="M23.298,44.5c-0.1,0-0.1,0-0.2,0v73.2c0,3.3,2.7,6,6,6h65.5c3.3,0,6-2.7,6-6c0,0-0.1-73.2-0.2-73.2H23.298z M44.499,103 c0,2.801-2.2,5-5,5c-2.8,0-5-2.199-5-5V64.4c0-2.8,2.2-5,5-5c2.8,0,5,2.2,5,5V103z M66.898,103c0,2.801-2.2,5-5,5s-5-2.199-5-5 V64.4c0-2.8,2.2-5,5-5s5,2.2,5,5V103z M89.298,103c0,2.801-2.199,5-5,5c-2.799,0-5-2.199-5-5V64.4c0-2.8,2.201-5,5-5 c2.801,0,5,2.2,5,5V103z"></path> <path d="M100.499,20.1h-5.4h-5.4c-1.699,0-3-1.3-3-3V6.2c0-3.4-2.8-6.2-6.199-6.2H43.298c-3.4,0-6.2,2.8-6.2,6.2V17 c0,1.7-1.3,3-3,3h-5.4h-5.4c-3.4,0-6.2,2.8-6.2,6.2v2.1c0,3.4,2.7,6.1,6,6.2c0.1,0,0.1,0,0.2,0h77.1c0.101,0,0.101,0,0.2,0 c3.3-0.1,6-2.8,6-6.2v-2.1C106.698,22.8,103.898,20.1,100.499,20.1z M76.398,20L76.398,20h-28.9l0,0v-6.6c0-1.7,1.4-3.1,3.1-3.1 h22.699c1.701,0,3.101,1.4,3.101,3.1V20z"></path> </g> </g></svg>
                </div>
            </li>`;
        });
        wordBookList.innerHTML = bookHtml;
        /*wordContainer.innerHTML = bookHtml +
            `</ul>`;*/
            // `<div class="addWordList">
            //     <div class="wordCard">
            //         <a href="/mochilearn/page/addWordCard">단어장 추가</a>
            //     </div>
            // </div>`;

    }

    // 시작
    loadBooks();


});

function initializeListEventListener() {
    const wordBooks = document.querySelectorAll('li');
    console.log(wordBooks);
    wordBooks.forEach( book => {
        const deleteBtn = book.querySelector('.deleteBtn');

        deleteBtn.addEventListener('click', (e) => {

            const bookId = book.getAttribute('data-book-id');
            console.log('단어장 삭제 클릭');
            console.log('삭제할 단어장: ' + bookId);

            if(confirm('단어장을 삭제하시겠습니까?')) {

                removeBook(bookId);
            }
        });
    });
}

function removeBook(bookId) {

    fetch(`/mochilearn/api/wordbook/delete?bookId=${bookId}`,
        {
            method: 'DELETE',
            headers: {'Content-Type': 'application/json'}
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            }
        })
        .then(data => {
            console.log('단어장 삭제 성공', data);
            window.location.href = "../page/word";
        })
        .catch(error => {
            console.error('Error', error);
            alert('단어장 삭제에 실패했습니다.');
        });
}