package com.alexb.main
package context

import com.alexb.infinispan.InfinispanCacheManager
import org.infinispan.manager.DefaultCacheManager

trait InfinispanSupport extends Caching {
  this: Configuration =>

  val cacheManager = new InfinispanCacheManager(new DefaultCacheManager(config.getString("infinispan.config")))
}