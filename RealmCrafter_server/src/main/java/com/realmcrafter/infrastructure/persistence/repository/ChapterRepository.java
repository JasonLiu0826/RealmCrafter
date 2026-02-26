package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<ChapterDO, Long> {

    List<ChapterDO> findByStoryIdOrderByChapterIndexAsc(String storyId);

    Optional<ChapterDO> findByStoryIdAndChapterIndex(String storyId, Integer chapterIndex);
}
