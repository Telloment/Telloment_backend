package cau.capstone.backend.User.service;


import cau.capstone.backend.User.dto.request.UpdateUserDto;
import cau.capstone.backend.User.model.Follow;
import cau.capstone.backend.User.model.User;
import cau.capstone.backend.User.dto.response.ResponseSearchUserDto;
import cau.capstone.backend.User.dto.response.ResponseSimpleUserDto;
import cau.capstone.backend.User.model.repository.FollowRepository;
import cau.capstone.backend.User.model.repository.UserRepository;
import cau.capstone.backend.global.security.Entity.JwtTokenProvider;
import cau.capstone.backend.global.security.SecurityUtil;
import cau.capstone.backend.global.security.dto.CreateUserDto;
import cau.capstone.backend.global.security.dto.ResponseUserDto;
import cau.capstone.backend.global.util.api.ResponseCode;
import cau.capstone.backend.global.util.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final JwtTokenProvider jwtTokenProvider;


    @Transactional
    public long saveUser(CreateUserDto createUserDto) {
        if (userRepository.existsByName(createUserDto.getName())) {
            log.info("이미 존재하는 닉네임입니다 by {}", createUserDto.getName());
            throw new UserException(ResponseCode.USER_NAME_ALREADY_EXIST);
        }
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            log.info("이미 존재하는 이메일입니다 by {}", createUserDto.getEmail());
            throw new UserException(ResponseCode.USER_EMAIL_ALREADY_EXIST);
        }

        User user = User.createUser(createUserDto.getEmail(), createUserDto.getPassword(),createUserDto.getName(), createUserDto.getNickname());

        return userRepository.save(user).getId();
    }

    // 현재 SecurityContext 에 있는 유저 정보 가져와 유저 정보 반환
    public ResponseUserDto getMyInfo() {
        return userRepository.findById(SecurityUtil.getCurrentMemberId())
                .map(ResponseUserDto::of)
                .orElseThrow(() -> new RuntimeException("로그인 유저 정보가 없습니다."));
    }

    // 회원정보 조회

    @Transactional(readOnly = true)
    public ResponseUserDto getUserInfo(String email) {
        return userRepository.findByEmail(email)
                .map(ResponseUserDto::of)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ResponseUserDto getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .map(ResponseUserDto::of)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND));
    }


  //   회원정보 수정
    @Transactional
    public ResponseUserDto updateUserInfo(UpdateUserDto updateUserDto, String accessToken) {
        Long userId = jwtTokenProvider.getUserPk(accessToken);

        User user = getUserById(userId);
        log.info("{} 회원정보 조회 완료: ", user.getName());

        user.updateUser(updateUserDto.getName(), updateUserDto.getImage(), updateUserDto.getName(), updateUserDto.getNickname() );
        userRepository.save(user);
        log.info("{} 회원정보 수정 완료: ", user.getName());

        ResponseUserDto responseUserDto = ResponseUserDto.of(user);
        return responseUserDto;
    }


    //for test
    @Transactional
    public ResponseUserDto updateUserInfo(UpdateUserDto updateUserDto) {
        Long userId = 1L;

        User user = getUserById(userId);
        log.info("{} 회원정보 조회 완료: ", user.getName());

        user.updateUser(updateUserDto.getName(), updateUserDto.getImage(), updateUserDto.getName(), updateUserDto.getNickname() );
        userRepository.save(user);
        log.info("{} 회원정보 수정 완료: ", user.getName());

        ResponseUserDto responseUserDto = ResponseUserDto.of(user);
        return responseUserDto;
    }





    // 회원 탈퇴
    // @CacheEvict(value = {"ResponseSimpleUserDto", "ResponseUserDto", "ResponseUserNutritionDto"}, key = "#userId", cacheManager = "diareatCacheManager")
    @Transactional
    public long deleteUser(String accessToken) {
        Long userId = jwtTokenProvider.getUserPk(accessToken);

        validateUser(userId);
        userRepository.deleteById(userId);
        log.info("PK {} 회원 탈퇴 완료: ", userId);

        return userId;
    }

    //for test
    @Transactional
    public long deleteUser(Long userId) {
        validateUser(userId);
        userRepository.deleteById(userId);
        log.info("PK {} 회원 탈퇴 완료: ", userId);

        return userId;
    }



    //팔로우하는 특정 회원 검색
    @Transactional(readOnly = true)
    public List<ResponseSearchUserDto> searchUser(String accessToken, String name) {
        Long hostId = jwtTokenProvider.getUserPk(accessToken);

        validateUser(hostId);
        log.info("{} 회원 검증 완료", hostId);
        List<User> users = new ArrayList<>(userRepository.findAllByNameContaining(name));
        log.info("{} 검색 결과 조회 완료", name);
        users.removeIf(user -> user.getId().equals(hostId)); // 검색 결과에서 자기 자신은 제외 (removeIf 메서드는 ArrayList에만 존재)
        return users.stream()
                .map(user -> ResponseSearchUserDto.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .image(user.getImage())
                        .isFollow(followRepository.existsByFollowerIdAndFolloweeId(hostId, user.getId()))
                        .build()).collect(Collectors.toList());
    }


    //for test
    @Transactional(readOnly = true)
    public List<ResponseSearchUserDto> searchUser(Long hostId, String name) {

        validateUser(hostId);
        log.info("{} 회원 검증 완료", hostId);
        List<User> users = new ArrayList<>(userRepository.findAllByNameContaining(name));
        log.info("{} 검색 결과 조회 완료", name);
        users.removeIf(user -> user.getId().equals(hostId)); // 검색 결과에서 자기 자신은 제외 (removeIf 메서드는 ArrayList에만 존재)
        return users.stream()
                .map(user -> ResponseSearchUserDto.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .image(user.getImage())
                        .isFollow(followRepository.existsByFollowerIdAndFolloweeId(hostId, user.getId()))
                        .build()).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<ResponseSearchUserDto> searchUser(String keyword) {
        List<User> userList = userRepository.findAllByNameContaining(keyword);
        log.info("검색 결과: {}", userList.size());
        return userList.stream()
                .map(ResponseSearchUserDto::of)
                .collect(Collectors.toList());
    }




    // 회원이 특정 회원 팔로우
    @Transactional
    public void followUser(String accessToken, Long followeeId) {
        Long followerId = jwtTokenProvider.getUserPk(accessToken);

        validateUser(followerId);
        validateUser(followeeId);

        // 이미 팔로우 중인 경우
        if (followRepository.existsByFolloweeIdAndFollowerId(followerId, followeeId)) {
            log.info("{}는 이미 {}를 팔로우한 상태입니다.", followerId, followeeId);
            throw new UserException(ResponseCode.FOLLOWED_ALREADY);
        }

        User followee = getUserById(followeeId); // 팔로우 대상이 존재하는지 확인 (toId가 존재하지 않으면 예외 발생
        User follower = getUserById(followerId); // 팔로우 요청자가 존재하는지 확인 (fromId가 존재하지 않으면 예외 발생
        followRepository.save(Follow.createFollow(followee, follower));
        log.info("이제 {}가 {}를 팔로우합니다.", follower, followee);

    }

    @Transactional
    public void followUser(Long followerId, Long followeeId) {

        validateUser(followerId);
        validateUser(followeeId);

        // 이미 팔로우 중인 경우
        if (followRepository.existsByFolloweeIdAndFollowerId(followerId, followeeId)) {
            log.info("{}는 이미 {}를 팔로우한 상태입니다.", followerId, followeeId);
            throw new UserException(ResponseCode.FOLLOWED_ALREADY);
        }

        User followee = getUserById(followeeId); // 팔로우 대상이 존재하는지 확인 (toId가 존재하지 않으면 예외 발생
        User follower = getUserById(followerId); // 팔로우 요청자가 존재하는지 확인 (fromId가 존재하지 않으면 예외 발생
        followRepository.save(Follow.createFollow(followee, follower));
        log.info("이제 {}가 {}를 팔로우합니다.", follower, followee);

    }

    // 회원이 특정 회원 팔로우 취소
    @Transactional
    public void unfollowUser(String accessToken, Long followeeId) {
        Long followerId = jwtTokenProvider.getUserPk(accessToken);

        validateUser(followeeId);
        validateUser(followerId);

        // 이미 팔로우 취소한 경우
        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            log.info("{}는 이미 {}를 팔로우 취소한 상태입니다.", followerId, followeeId);
            throw new UserException(ResponseCode.UNFOLLOWED_ALREADY);
        }
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        log.info("이제 {}가 {}를 언팔로우합니다.", followerId, followeeId);
    }
    //for test
    @Transactional
    public void unfollowUser(Long followerId, Long followeeId) {

        validateUser(followeeId);
        validateUser(followerId);

        // 이미 팔로우 취소한 경우
        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            log.info("{}는 이미 {}를 팔로우 취소한 상태입니다.", followerId, followeeId);
            throw new UserException(ResponseCode.UNFOLLOWED_ALREADY);
        }
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        log.info("이제 {}가 {}를 언팔로우합니다.", followerId, followeeId);
    }


    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId))
            throw new UserException(ResponseCode.USER_NOT_FOUND);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND));
    }
}