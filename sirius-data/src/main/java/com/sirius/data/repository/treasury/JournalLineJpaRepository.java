package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalLineJpaRepository extends JpaRepository<JournalLineEntity, UUID> {
}
