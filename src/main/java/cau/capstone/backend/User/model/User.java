package cau.capstone.backend.User.model;

import cau.capstone.backend.page.model.Like;
import cau.capstone.backend.page.model.Page;
import cau.capstone.backend.page.model.Book;
import cau.capstone.backend.global.Authority;
import cau.capstone.backend.global.BaseEntity;
import lombok.*;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;


    @Column(name = "user_email")
    private String email; //닉네임

    @Column(name = "user_passwd")
    private String passwd; //비밀번호

    @Column(name = "user_image")
    private String image; //프로필 이미지

    @Column(name = "user_name")
    private String name; //이름

    @Column(name = "user_nickname")
    private String nickname; //닉네임

    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private Authority role = Authority.USER; //권한

    @Column
    private LocalDateTime lastLoginAt; //마지막 로그인 시간

    @Setter
    @Column(name = "voice_use_permission_flag")
    @ColumnDefault("false")
    private boolean voiceUsePermissionFlag = false; //음성 사용 권한

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Page> pages = new ArrayList<>();


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Like> likes = new ArrayList<>();


    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();


    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Score score;

    @Builder
    public User(String email, String passwd, Authority role) {
        this.email = email;
        this.passwd = passwd;
        this.role = role;
    }


    // Jwt 전용 설정 (UserDetails 인터페이스 구현)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<Authority> auth = Collections.singletonList(this.role);
        return auth.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString())) // 'ROLE_' 접두사 추가
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return this.passwd;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean getVoiceUsePermissionFlag() {
        return this.voiceUsePermissionFlag;
    }


    // Jwt 전용 설정 종료

    //생성 메서드
    public static User createUser(String email, String passwd, String name, String nickname) {
        User user = new User();
        user.email = email;
        user.passwd = passwd;
        user.name = name;
        user.nickname = nickname;
        user.role = Authority.USER;

        user.score = new Score(user);
        return user;
    }

    public void updateUser(String email, String image, String name, String nickname) {
        this.email = email;
        this.name = name;
        this.image = image;
        this.nickname = nickname;
    }

    public void addBook(Book book) {
        this.books.add(book);
    }

    public void addLike(Like like) {
        this.likes.add(like);
    }

    public void removeLike(Like like) {
        this.likes.remove(like);
    }

    public void setId(Long id) {
        this.id = id;
    }


}
