// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.actions;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.platform.PlatformInfo;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.AspectDescriptor;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.SerializationConstant;
import javax.annotation.Nullable;
import net.starlark.java.syntax.Location;

/**
 * Contains metadata used for reporting the progress and status of an action.
 *
 * <p>Morally an action's owner is the RuleConfiguredTarget instance responsible for creating it,
 * but to avoid storing heavyweight analysis objects in actions, and to avoid coupling between the
 * analysis and actions packages, the RuleConfiguredTarget provides an instance of this class.
 */
// TODO(b/274783642): Instead of storing `mnemonic`, `configurationChecksum`,
//  `buildConfigurationEvent` and `additionalProgressInfo` fields, we can instead just store
//  `BuildConfigurationValue` field in `ActionOwner` class, which saves 3 fields.
@Immutable
public abstract class ActionOwner {
  /** An action owner for special cases. Usage is strongly discouraged. */
  @SerializationConstant
  public static final ActionOwner SYSTEM_ACTION_OWNER =
      create(
          /* label= */ null,
          Location.BUILTIN,
          /* targetKind= */ "empty target kind",
          /* mnemonic= */ "system",
          /* configurationChecksum= */ "system",
          /* buildConfigurationEvent= */ null,
          /* additionalProgressInfo= */ null,
          /* executionPlatform= */ null,
          /* aspectDescriptors= */ ImmutableList.of(),
          /* execProperties= */ ImmutableMap.of());

  public static ActionOwner create(
      @Nullable Label label,
      Location location,
      String targetKind,
      String mnemonic,
      String configurationChecksum,
      @Nullable BuildConfigurationEvent buildConfigurationEvent,
      @Nullable String additionalProgressInfo,
      @Nullable PlatformInfo executionPlatform,
      ImmutableList<AspectDescriptor> aspectDescriptors,
      ImmutableMap<String, String> execProperties) {
    if (aspectDescriptors.isEmpty() && execProperties.isEmpty()) {
      return LiteActionOwner.createInternal(
          label,
          location,
          targetKind,
          mnemonic,
          configurationChecksum,
          buildConfigurationEvent,
          additionalProgressInfo,
          executionPlatform);
    } else {
      return FullActionOwner.createInternal(
          label,
          location,
          targetKind,
          mnemonic,
          configurationChecksum,
          buildConfigurationEvent,
          additionalProgressInfo,
          executionPlatform,
          aspectDescriptors,
          execProperties);
    }
  }

  /** Returns the label for this ActionOwner, or null if the {@link #SYSTEM_ACTION_OWNER}. */
  @Nullable
  public abstract Label getLabel();

  /** Returns the location for this ActionOwner. */
  public abstract Location getLocation();

  /** Returns the target kind (rule class name) for this ActionOwner. */
  public abstract String getTargetKind();

  /** Returns the mnemonic for the configuration of the action owner. */
  public abstract String getMnemonic();

  /**
   * Returns the short cache key for the configuration of the action owner.
   *
   * <p>Special action owners that are not targets can return any string here. If the underlying
   * configuration is null, this should return "null".
   */
  public abstract String getConfigurationChecksum();

  /**
   * Return the {@link BuildConfigurationEvent} associated with the action owner, if any, as it
   * should be reported in the build event protocol.
   */
  @Nullable
  public abstract BuildConfigurationEvent getBuildConfigurationEvent();

  /**
   * Returns additional information that should be displayed in progress messages, or {@code null}
   * if nothing should be added.
   */
  @Nullable
  abstract String getAdditionalProgressInfo();

  /**
   * Returns the {@link PlatformInfo} platform this action should be executed on. If the execution
   * platform is {@code null}, then the host platform is assumed.
   */
  @Nullable
  public abstract PlatformInfo getExecutionPlatform();

  public abstract ImmutableList<AspectDescriptor> getAspectDescriptors();

  /** Returns a String to String map containing the execution properties of this action. */
  @VisibleForTesting
  public abstract ImmutableMap<String, String> getExecProperties();

  /**
   * Created when {@code aspectDescriptors} and {@code execProperties} are both empty.
   *
   * <p>{@link LiteActionOwner} is more likely to be created since both fields above are usually
   * empty. This will save 8 bytes of memory for each {@link ActionOwner} instance compared to
   * keeping both empty fields.
   */
  @AutoValue
  abstract static class LiteActionOwner extends ActionOwner {
    static LiteActionOwner createInternal(
        @Nullable Label label,
        Location location,
        String targetKind,
        String mnemonic,
        String configurationChecksum,
        @Nullable BuildConfigurationEvent buildConfigurationEvent,
        @Nullable String additionalProgressInfo,
        @Nullable PlatformInfo executionPlatform) {
      return new AutoValue_ActionOwner_LiteActionOwner(
          label,
          location,
          targetKind,
          mnemonic,
          configurationChecksum,
          buildConfigurationEvent,
          additionalProgressInfo,
          executionPlatform);
    }

    @Override
    public final ImmutableList<AspectDescriptor> getAspectDescriptors() {
      return ImmutableList.of();
    }

    @Override
    public final ImmutableMap<String, String> getExecProperties() {
      return ImmutableMap.of();
    }
  }

  /** Created when either {@code aspectDescriptors} or {@code execProperties} is not empty. */
  @AutoValue
  abstract static class FullActionOwner extends ActionOwner {
    static FullActionOwner createInternal(
        @Nullable Label label,
        Location location,
        String targetKind,
        String mnemonic,
        String configurationChecksum,
        @Nullable BuildConfigurationEvent buildConfigurationEvent,
        @Nullable String additionalProgressInfo,
        @Nullable PlatformInfo executionPlatform,
        ImmutableList<AspectDescriptor> aspectDescriptors,
        ImmutableMap<String, String> execProperties) {
      return new AutoValue_ActionOwner_FullActionOwner(
          label,
          location,
          targetKind,
          mnemonic,
          configurationChecksum,
          buildConfigurationEvent,
          additionalProgressInfo,
          executionPlatform,
          aspectDescriptors,
          execProperties);
    }
  }
}
