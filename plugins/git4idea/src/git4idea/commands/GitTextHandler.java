/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package git4idea.commands;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.RunnerMediator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.Charset;

/**
 * The handler for git commands with text outputs
 */
public abstract class GitTextHandler extends GitHandler {
  /**
   * wrapped process handler
   */
  // note that access is safe because it accessed in unsynchronized block only after process is started, and it does not change after that
  @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"}) private OSProcessHandler myHandler;

  protected GitTextHandler(@NotNull Project project, @NotNull File directory, @NotNull GitCommand command) {
    super(project, directory, command);
  }

  protected GitTextHandler(final Project project, final VirtualFile vcsRoot, final GitCommand command) {
    super(project, vcsRoot, command);
  }

  @Override
  protected Process startProcess() throws ExecutionException {
    final ProcessHandler processHandler = new MyRunnerMediator().createProcess(myCommandLine);
    assert processHandler instanceof MyOSProcessHandler;
    myHandler = (MyOSProcessHandler)processHandler;
    return myHandler.getProcess();
  }

  protected void startHandlingStreams() {
    myHandler.addProcessListener(new ProcessListener() {
      public void startNotified(final ProcessEvent event) {
        // do nothing
      }

      public void processTerminated(final ProcessEvent event) {
        final int exitCode = event.getExitCode();
        setExitCode(exitCode);
        cleanupEnv();
        GitTextHandler.this.processTerminated(exitCode);
        listeners().processTerminated(exitCode);
      }

      public void processWillTerminate(final ProcessEvent event, final boolean willBeDestroyed) {
        // do nothing
      }

      public void onTextAvailable(final ProcessEvent event, final Key outputType) {
        GitTextHandler.this.onTextAvailable(event.getText(), outputType);
      }
    });
    myHandler.startNotify();
  }

  /**
   * Notification for handler to handle process exit event
   *
   * @param exitCode a exit code.
   */
  protected abstract void processTerminated(int exitCode);

  /**
   * This method is invoked when some text is available
   *
   * @param text       an available text
   * @param outputType output type
   */
  protected abstract void onTextAvailable(final String text, final Key outputType);

  public void destroyProcess() {
    myHandler.destroyProcess();
  }

  protected void waitForProcess() {
    myHandler.waitFor();
  }

  /**
   * RunnerMediator that attaches our MyProcessHandler to the process instead of the standard CustomDestroyProcessHandler.
   */
  private class MyRunnerMediator extends RunnerMediator {
    @Override
    protected ProcessHandler createProcessHandler(@NotNull Process process, @NotNull GeneralCommandLine commandLine) {
      return new MyOSProcessHandler(process, commandLine, getCharset());
    }
  }

  private static class MyOSProcessHandler extends RunnerMediator.CustomDestroyProcessHandler {
    private final Charset myCharset;

    public MyOSProcessHandler(Process process, GeneralCommandLine commandLine, Charset charset) {
      super(process, commandLine);
      myCharset = charset;
    }

    @Override
    public Charset getCharset() {
      Charset charset = myCharset;
      return charset == null ? super.getCharset() : charset;
    }
  }
}
