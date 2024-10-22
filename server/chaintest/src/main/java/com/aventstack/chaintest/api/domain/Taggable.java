package com.aventstack.chaintest.api.domain;

import com.aventstack.chaintest.api.tag.Tag;
import java.util.Set;

public interface Taggable {

    Set<Tag> getTags();

}
