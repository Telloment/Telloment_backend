package cau.capstone.backend.page.service;


import cau.capstone.backend.page.model.Category;
import cau.capstone.backend.User.service.ScoreService;
import cau.capstone.backend.global.redis.RankingService;
import cau.capstone.backend.global.security.Entity.JwtTokenProvider;
import cau.capstone.backend.page.dto.request.*;
import cau.capstone.backend.page.dto.response.ResponsePageDto;
import cau.capstone.backend.page.model.*;
import cau.capstone.backend.page.model.repository.*;
import cau.capstone.backend.User.model.User;
import cau.capstone.backend.User.model.repository.UserRepository;
import cau.capstone.backend.global.util.api.ResponseCode;
import cau.capstone.backend.global.util.exception.LikeException;
import cau.capstone.backend.global.util.exception.PageException;
import cau.capstone.backend.global.util.exception.ScrapException;
import cau.capstone.backend.global.util.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PageService {

    private final PageRepository pageRepository; //
    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final BookRepository bookRepository;

    private final JwtTokenProvider jwtTokenProvider;

    private final ScoreService scoreService;

    private final RankingService rankingService;
    private final HashtagRepository hashtagRepository;


    @Transactional
    public ResponsePageDto getPage(String  accessToken, Long pageId) {
        Long userId = jwtTokenProvider.getUserPk(accessToken);
        Page page = getPageById(pageId);
        page.setViewCount(page.getViewCount() + 1);
        pageRepository.save(page);

//        rankingService.viewPage(page.getId(), page.getCategory());
        scoreService.plusViewScore(userId, page);

        return ResponsePageDto.from(page);
}

    public org.springframework.data.domain.Page<Page> searchPagesWithKeywordAndPaging(String keyword, Pageable pageable) {
        return pageRepository.findByKeywordWithPaging(keyword, pageable);
    }

    @Transactional
    public ResponsePageDto setPageEmotion(Long pageId, String emotion){
        Page page = getPageById(pageId);
        page.setEmotion(emotion);
        pageRepository.save(page);

        return ResponsePageDto.from(page);
    }

    @Transactional
    public long createPage(CreatePageDto createPageDto, String accessToken) {
        Long userId = jwtTokenProvider.getUserPk(accessToken);
        User user = getUserById(userId);
        Book book = getBookById(createPageDto.getBookId());

        Page page = Page.createPage(user, book,  createPageDto.getTitle(), createPageDto.getContent());
        log.info("new Page saved: " + page.getTitle());
        return pageRepository.save(page).getId();
    }

    @Transactional
    public List<ResponsePageDto> getPageList(Long userId){
        validateUser(userId);
        List<Page> pageList = pageRepository.findAllByUserId(userId);
        log.info("Page list returned: " + pageList.size());

        List<ResponsePageDto> responsePageDtoList = pageList.stream()
                .map(ResponsePageDto::from)
                .collect(Collectors.toList());

        return responsePageDtoList;
    }

    //페이지 정보 수정
    @Transactional
    public long updatePage(UpdatePageDto updatePageDto){
        Page page = getPageById(updatePageDto.getPageId());
        page.updatePage(updatePageDto.getTitle(), updatePageDto.getContent());
        log.info("Page updated: " + page.getTitle());

        return pageRepository.save(page).getId();
    }

    //페이지 삭제
    @Transactional
    public long deletePage(Long pageId, Long userId){
        validatePage(pageId, userId);
        pageRepository.deleteById(pageId);
        log.info("Page deleted: " + pageId);

        return pageId;
    }





    @Transactional
    public List<ResponsePageDto> getPageListByDate(Long userId, LocalDate date){
        List<Page> pageList = pageRepository.findAllByUserIdAndCreatedAt(userId, date);

        List<ResponsePageDto> responsePageDtoList = pageList.stream()
                .map(ResponsePageDto::from)
                .collect(Collectors.toList());

        log.info("Page list returned: " + pageList.size());
        return responsePageDtoList;
    }


    //페이지 연결하기
    @Transactional
    public long linkPage(LinkPageDto linkPageDto){

        Long prevPageId = linkPageDto.getPrevPageId();
        Long nextPageId = linkPageDto.getNextPageId();


        Page prevPage = getPageById(prevPageId);
        Page linkedPage = getPageById(nextPageId);

        validatePage(prevPage.getId(), prevPage.getUser().getId()) ;
        validatePage(linkedPage.getId(), linkedPage.getUser().getId());

        //첫 페이지 바로 다음에 연결
        if (prevPage.getRootId() == -1){
            prevPage.setNextId(linkedPage);
            linkedPage.setRootId(prevPage);
            linkedPage.setPrevId(prevPage);

            pageRepository.save(prevPage);
            pageRepository.save(linkedPage);
        }

        //페이지 연결 순서가 세 번째 이상
        else if(prevPage.getRootId() != -1){
            Page rootPage = pageRepository.findById(prevPage.getRootId())
                    .orElseThrow(() -> new PageException(ResponseCode.PAGE_NOT_FOUND));

            prevPage.setNextId(linkedPage);
            linkedPage.setPrevId(prevPage);
            linkedPage.setRootId(rootPage);

            pageRepository.save(prevPage);
            pageRepository.save(linkedPage);
        }

        return linkedPage.getId();
    }


    //페이지 좋아요
    @Transactional
    public long likePage(LikePageDto likePageDto, String accessToken){

        Long userId = jwtTokenProvider.getUserPk(accessToken);

        Page page = getPageById(likePageDto.getPageId());
        Book book = getBookById(page.getBook().getId());
        User user = getUserById(userId);

        Like like = Like.createLike(user, page);

        List<Like> likes = page.getLikes();
        likes.add(like);
        page.setLikes(likes);

        Category category = book.getCategory();
        scoreService.plusLikeScore(userId, page);
        rankingService.likePage(page.getId(), category);

        likeRepository.save(like);
        pageRepository.save(page);

        return page.getId();
    }

    //좋아요 해제
    @Transactional
    public long dislikePage(LikePageDto likePageDto, String accessToken){


        Long userId = jwtTokenProvider.getUserPk(accessToken);

        Page page = getPageById(likePageDto.getPageId());
        Book book = getBookById(page.getBook().getId());

        Like like = likeRepository.findByUserIdAndPageId(userId, likePageDto.getPageId());

        if(like == null){
            throw new LikeException(ResponseCode.LIKE_NOT_FOUND);
        }
        else{
            List<Like> likes = page.getLikes();
            likes.remove(like);
            page.setLikes(likes);

            Category category = book.getCategory();
            rankingService.unlikePage(page.getId(), category);

            likeRepository.delete(like);
            pageRepository.save(page);

            return page.getId();
        }


    }

    public List<Page> getPagesByHashtag(String hashtag) {
        return hashtagRepository.findPagesByHashtag(hashtag);
    }

    @Transactional
    public Page createPageWithHashtags(Page page, Set<String> hashtags) {
        Set<Hashtag> hashtagEntities = new HashSet<>();

        for (String tag : hashtags) {
            Hashtag hashtag = hashtagRepository.findByTag(tag)
                    .orElseGet(() -> hashtagRepository.save(new Hashtag(tag)));
            hashtagEntities.add(hashtag);
        }

        page.setHashtags(hashtagEntities);
        return pageRepository.save(page);
    }

    @Transactional
    public void setHashtagsToPage(Page page, Set<String> hashtags) {
        Set<Hashtag> existingHashtags = page.getHashtags();

        for (String tag : hashtags) {
            Hashtag existingHashtag = hashtagRepository.findByTag(tag)
                    .orElseGet(() -> new Hashtag(tag));
            existingHashtags.add(existingHashtag);
        }

        page.setHashtags(existingHashtags);
        pageRepository.save(page);
    }



    



    @Transactional
    public List<Page> getUserPageList(Long userId){
        return pageRepository.findAllByUserId(userId);
    }

    @Transactional
    public List<Like> getUserLikeList(Long userId){
        return likeRepository.findAllByUserId(userId);
    }

    @Transactional
    public List<Scrap> getUserScrapList(Long userId){
        return scrapRepository.findAllByUserId(userId);
    }

    @Transactional
    public List<Book> getUserBookList(Long userId){
        return bookRepository.findAllByUserId(userId);
    }




    private User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND));
    }

    private Page getPageById(Long pageId){
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new PageException(ResponseCode.PAGE_NOT_FOUND));
    }

    private Book getBookById(Long bookId){
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new PageException(ResponseCode.BOOK_NOT_FOUND));
    }

    private Scrap getScrapById(Long scrapId){
        return scrapRepository.findById(scrapId)
                .orElseThrow(() -> new ScrapException(ResponseCode.SCRAP_NOT_FOUND));
    }


    private void validateUser(Long userId){
        if(!userRepository.existsById(userId)){
            throw new UserException(ResponseCode.USER_NOT_FOUND);
        }
    }

    private void validatePage(Long pageId){
        if(!pageRepository.existsById(pageId)){
            throw new PageException(ResponseCode.PAGE_NOT_FOUND);
        }

    }

    private void validatePage(Long pageId, Long userId){
        if(!pageRepository.existsById(pageId)){
            throw new PageException(ResponseCode.PAGE_NOT_FOUND);
        }
        if (!pageRepository.existsByIdAndUserId(pageId, userId)){ //페이지의 주인이 아닌지 확인
            throw new PageException(ResponseCode.PAGE_NOT_OWNED);
        }
    }

    private void validateScrap(Long scrapId, Long userId){
        if(!scrapRepository.existsById(scrapId)){
            throw new ScrapException(ResponseCode.SCRAP_NOT_FOUND);
        }
        if(!scrapRepository.existsByIdAndUserId(scrapId, userId)){ //유저가 스크랩 한 게 맞는지 확인
            throw new ScrapException(ResponseCode.SCARP_NOT_OWNED);
        }
    }
}
