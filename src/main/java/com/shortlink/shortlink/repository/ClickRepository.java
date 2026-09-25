package com.shortlink.shortlink.repository;

import com.shortlink.shortlink.entity.Click;
import com.shortlink.shortlink.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    long countByUrl(Url url);

    Optional<Click> findTopByUrlOrderByAccessedAtDesc(Url url);
}
