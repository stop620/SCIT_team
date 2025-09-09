document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM 로드')
    let books = [];
    let memberId = 1;

    const wordContainer = document.querySelector('.wordContainer');

    // 단어장 목록 로딩
    function loadBooks() {
        console.log(`유저 ${memberId}의 단어장 목록 로드`);
        const storedBooks = sessionStorage.getItem('books');

        if(storedBooks) {
            books = JSON.parse(storedBooks);
            renderBooks();
        } else {
            console.log('단어장 요청');
            fetch(`/mochilearn/api/wordbook/list`)
                .then(response => response.json())
                .then(data => {
                    console.log('단어장 받음');

                    books = data.data;
                    console.log(books);

                    sessionStorage.setItem('books', JSON.stringify(data.data));

                    renderBooks();
                })
        }
    }

    // 단어장 렌더링
    function renderBooks() {

        let bookHtml = '';

        books.forEach(book => {
            bookHtml += `
                <div class="wordList">
                <div class="wordCard">
                    <a th:href="@{/page/wordCard2}">${book.title}</a>
                </div>
            </div>
            `;
            wordContainer.innerHTML += bookHtml;
        });

        wordContainer.innerHTML +=
            `<div class="addWordList">
                <div class="wordCard">
                    <a href="/mochilearn/page/addWordCard">단어장 추가</a>
                </div>
            </div>`;

    }

    // 시작
    loadBooks();
});