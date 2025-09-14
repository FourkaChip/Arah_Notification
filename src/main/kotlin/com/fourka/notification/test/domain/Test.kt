package com.fourka.notification.test.domain

import com.fourka.notification.shared.BaseTimeEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("test")
class Test(
    @Id
    var id: Long? = null,

    var name: String
) : BaseTimeEntity()