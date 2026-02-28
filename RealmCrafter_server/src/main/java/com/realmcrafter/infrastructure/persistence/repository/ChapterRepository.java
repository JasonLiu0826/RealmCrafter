package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<ChapterDO, Long> {

    List<ChapterDO> findByStoryIdOrderByChapterIndexAsc(String storyId);

    /** 取该故事最新 N 章（按序号降序），用于 L2 短期记忆滑动窗口 */
    List<ChapterDO> findTop5ByStoryIdOrderByChapterIndexDesc(String storyId);

    Optional<ChapterDO> findByStoryIdAndChapterIndex(String storyId, Integer chapterIndex);
}
