/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef SHARE_GC_Z_ZMARKCONTEXT_INLINE_HPP
#define SHARE_GC_Z_ZMARKCONTEXT_INLINE_HPP

#include "classfile/javaClasses.inline.hpp"
#include "gc/z/zMarkCache.inline.hpp"
#include "gc/z/zMarkContext.hpp"

inline ZMarkContext::ZMarkContext(size_t nstripes,
                                  ZMarkStripe* stripe,
                                  ZMarkThreadLocalStacks* stacks) :
    _cache(nstripes),
    _stripe(stripe),
    _stacks(stacks),
    _string_dedup_requests() {}

inline ZMarkStripe* ZMarkContext::stripe() {
  return _stripe;
}

inline ZMarkThreadLocalStacks* ZMarkContext::stacks() {
  return _stacks;
}

inline void ZMarkContext::inc_live(ZPage* page, size_t bytes) {
  _cache.inc_live(page, bytes);
}

inline void ZMarkContext::try_deduplicate(oop obj) {
  if (!StringDedup::is_enabled()) {
    // Not enabled
    return;
  }

  if (!java_lang_String::is_instance_inlined(obj)) {
    // Not a String object
    return;
  }

  if (java_lang_String::test_and_set_deduplication_requested(obj)) {
    // Already deduplicated
    return;
  }

  // Request deduplication
  _string_dedup_requests.add(obj);
}

#endif // SHARE_GC_Z_ZMARKCACHE_INLINE_HPP
