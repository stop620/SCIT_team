document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM 로드')
    let books = [];

    const wordContainer = document.querySelector('.wordContainer');

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
            })
    }


    // 단어장 렌더링
    function renderBooks() {

        let bookHtml = '';
        
        bookHtml += `<div class="addWordCard">
                <a href="/mochilearn/page/addWordCard">단어장 추가</a>
            </div>
            <ul>`;

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
            // 단어 갯수을 어떻게 가져올지?

            bookHtml += `
            <li>
                <div class="wordCardList">
                    <a class="wordTitle" href="/mochilearn/page/wordCard?bookId=${book.id}">${book.title}</a>
                    <a class="wordCount"></a>
                    <a class="deleteBook">
                        <img src="/mochilearn/img/trash-bin.png" class="deleteBtn" data-book-id="${book.id}">
                    </a>
                </div>
            </li>`;
        });
        wordContainer.innerHTML = bookHtml +
            `</ul>`;
            // `<div class="addWordList">
            //     <div class="wordCard">
            //         <a href="/mochilearn/page/addWordCard">단어장 추가</a>
            //     </div>
            // </div>`;

    }

    // 시작
    loadBooks();
});