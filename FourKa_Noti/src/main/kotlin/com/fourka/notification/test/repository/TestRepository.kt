package com.fourka.notification.test.repository

import com.fourka.notification.test.domain.Test
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TestRepository : CoroutineCrudRepository<Test, Long>