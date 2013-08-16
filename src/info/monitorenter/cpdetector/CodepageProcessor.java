/*
 * CodepageProcessor.java an executable command line interface 
 * for batch processing files with cpdetector.
 *
 * Copyright (C) Achim Westermann, created on 02.06.2004, 09:02:19.
 *  
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 * 
 * The contents of this collection are subject to the Mozilla Public License Version 
 * 1.1 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * The Original Code is the cpDetector code in [sub] packages info.monitorenter and 
 * cpdetector. 
 * 
 * The Initial Developer of the Original Code is
 * Achim Westermann <achim.westermann@gmx.de>.
 * 
 * Portions created by the Initial Developer are Copyright (c) 2007 
 * the Initial Developer. All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 * 
 * ***** END LICENSE BLOCK ***** * 
 */
/*
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  If you modify or optimize the code in a useful way please let me know.
 *  Achim.Westermann@gmx.de
 *
 */
package info.monitorenter.cpdetector;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.FileFilterExtensions;
import info.monitorenter.cpdetector.io.ICodepageDetector;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnknownCharset;
import info.monitorenter.cpdetector.reflect.SingletonLoader;
import info.monitorenter.util.FileUtil;
import jargs.gnu.CmdLineParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

/**
 * An executable command line interface for batch processing files with cpdetector.
 * <p>
 * It works on a <code>{@link info.monitorenter.cpdetector.io.CodepageDetectorProxy}</code> to
 * detect the charset encoding of documents and uses that information to sort the given documents in
 * a taxonomy tree that contains the codepage name at the root folders. Optionally the codepages may
 * be tried to transform to a specified target codepage.
 * <p>
 * <h3>Usage</h3> Two alternatives:
 * <ol>
 * <li>Deflated classfiles (not contained in a jar): <br>
 * <ul>
 * <li>Put the folder above top - level package <code>cpdetector</code> to the classpath.
 * <li>Invoke: <br>
 * <code> java -cp &lt;topfolder&gt; cpdetector.CodpageProcessor</code>
 * </ul>
 * <li>jarfile:
 * <ul>
 * <li>Invoke: <br>
 * <code> java -jar cpdetector.jar </code>
 * </ul>
 * <li>You will see a usage - text that informs about the parameters and their effect.
 * </ol>
 * <p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann </a>
 */
public class CodepageProcessor
    extends ACmdLineArgsInheritor {

  /**
   * The root folder (directory) under which all files for the collection are found.
   */
  protected File collectionRoot = null;

  /**
   * The codepage detection proxy that will be used. Is optionally configured by argument \"-c\".
   */
  protected CodepageDetectorProxy detector;

  /**
   * A list of all Charset implementations of this java version. Used for debug output.
   */
  private Charset[] parseCodepages;

  private static String fileseparator = System.getProperty("file.separator");

  /**
   * Needed for searching the collection root directory recursively for extensions.
   */
  private FileFilter extensionFilter;

  /**
   * Where does it go to. Has to be a directory.
   */
  private File outputDir;

  /**
   * Move unrecognized codepages to "unknown"?
   */
  private boolean moveUnknown = false;

  /** Flag to detect the -c option: only print an return. * */
  private boolean printCharsets = false;

  private boolean verbose = false;

  /**
   * Wait between processing documents. Default is zero.
   */
  private long wait = 0;

  /**
   * If set to a {@link Charset}, the documents won't be sorted by their codepage but tried to be
   * transformed. This may not be possible, if the detected charset is not supported by the current
   * VM. In that case it will be sorted to a subdir with the codepage name (as normal).
   */
  private Charset targetCodepage = null;

  /**
   * Internal buffer for codepage transformation (argument -t).
   */
  private static char[] transcodeBuffer = new char[1024];

  /**
   * Internal buffer for document transport.
   */
  private static byte[] rawtransportBuffer = new byte[1024];

  public CodepageProcessor() {
    super();
    this.detector = CodepageDetectorProxy.getInstance();
    // adding the options:
    this.addCmdLineOption("documents", new CmdLineParser.Option.StringOption('r', "documents"));
    this.addCmdLineOption("extensions", new CmdLineParser.Option.StringOption('e', "extensions"));
    this.addCmdLineOption("outputDir", new CmdLineParser.Option.StringOption('o', "outputDir"));
    this.addCmdLineOption("moveUnknown", new CmdLineParser.Option.BooleanOption('m', "moveUnknown"));
    this.addCmdLineOption("verbose", new CmdLineParser.Option.BooleanOption('v', "verbose"));
    this.addCmdLineOption("wait", new CmdLineParser.Option.IntegerOption('w', "wait"));
    this.addCmdLineOption("transform", new CmdLineParser.Option.StringOption('t', "transform"));
    this.addCmdLineOption("detectors", new CmdLineParser.Option.StringOption('d', "detectors"));
    this.addCmdLineOption("charsets", new CmdLineParser.Option.BooleanOption('c', "charsets"));
  }

  public void parseArgs(String[] cmdLineArgs) throws Exception {
    // Has to be first call!!
    super.parseArgs(cmdLineArgs);
    Object collectionOption = this.getParsedCmdLineOption("documents");
    Object extensionsOption = this.getParsedCmdLineOption("extensions");
    Object outputDirOption = this.getParsedCmdLineOption("outputDir");
    Object moveUnknownOption = this.getParsedCmdLineOption("moveUnknown");
    Object verboseOption = this.getParsedCmdLineOption("verbose");
    Object waitOption = this.getParsedCmdLineOption("wait");
    Object transformOption = this.getParsedCmdLineOption("transform");
    Object detectorOption = this.getParsedCmdLineOption("detectors");
    Object charsetsOption = this.getParsedCmdLineOption("charsets");

    if (charsetsOption != null) {
      this.printCharsets = ((Boolean) charsetsOption).booleanValue();
    } else {

      if (collectionOption == null) {
        usage();
        throw new MissingResourceException("Parameter for collection root directory is missing.",
            "String", "-r");
      }
      this.collectionRoot = new File(collectionOption.toString());
      if (outputDirOption == null) {
        usage();
        throw new MissingResourceException("Parameter for output directory is missing.", "String",
            "-o");
      }
      this.outputDir = new File(outputDirOption.toString());
      if (extensionsOption != null) {
        this.extensionFilter = new FileFilterExtensions(this.parseCSVList(extensionsOption
            .toString()));
      } else {
        // Anonymous dummy:
        this.extensionFilter = new FileFilter() {
          public boolean accept(File f) {
            return true;
          }
        };
      }
      if (moveUnknownOption != null) {
        this.moveUnknown = true;
      }
      if (verboseOption != null) {
        if (((Boolean) verboseOption).booleanValue()) {
          this.verbose = true;
        }
      }
      if (waitOption != null) {
        this.wait = ((Integer) waitOption).intValue() * 1000;
      }
      if (transformOption != null) {
        String charset = (String) transformOption;
        try {
          this.targetCodepage = Charset.forName(charset);
        } catch (Exception e) {
          StringBuffer msg = new StringBuffer();
          msg.append("Given charset name: \"");
          msg.append(charset);
          msg.append("\" for option -t is illegal: \n");
          msg.append("  ");
          msg.append(e.getMessage());
          msg.append("\n");
          msg.append("   Legal values are: \n");
          for (int i = 0; i < parseCodepages.length; i++) {
            msg.append("    ");
            msg.append(parseCodepages[i].name());
            msg.append("\n");
          }
          throw new IllegalArgumentException(msg.toString());
        }
      }
      if (detectorOption != null) {
        String[] detectors = this.parseCSVList((String) detectorOption);
        if (detectors.length == 0) {
          StringBuffer msg = new StringBuffer();
          msg.append("You specified the codepage detector argument \"-d\" but ommited any comma-separated fully qualified class-name.");
          throw new IllegalArgumentException(msg.toString());
        }

        // try to instantiate and cast:
        ICodepageDetector cpDetector = null;
        for (int i = 0; i < detectors.length; i++) {
          try {
            cpDetector = (ICodepageDetector) SingletonLoader.getInstance()
                .newInstance(detectors[i]);
            if (cpDetector != null) {
              this.detector.add(cpDetector);
            }
          } catch (InstantiationException ie) {
            System.err.println("Could not instantiate custom ICodepageDetector: " + detectors[i]
                + " (argument \"-c\"): " + ie.getMessage());
          }

        }
      }
      // default detector initialization:
      else {
        this.detector.add(new ParsingDetector(this.verbose));
        this.detector.add(JChardetFacade.getInstance());
      }
      this.loadCodepages();
    }
  }

  private void printCharsets() {
    if (this.parseCodepages == null || this.parseCodepages.length == 0) {
      this.loadCodepages();
    }
    Charset cs;
    for (int i = 0; i < this.parseCodepages.length; i++) {
      cs = this.parseCodepages[i];
      System.out.println("  " + cs.name() + ":");
      Set<String> aliases = cs.aliases();
      Iterator<String> itAliases = aliases.iterator();
      while (itAliases.hasNext()) {
        System.out.println("    " + itAliases.next());
      }
    }
  }

  private final String[] parseCSVList(String listLiteral) {
    if (listLiteral == null) {
      return null; // bounce bad callee.
    }
    List<String> tmpList = new LinkedList<String>();
    StringTokenizer tok = new StringTokenizer(listLiteral, ";,");
    while (tok.hasMoreElements()) {
      tmpList.add(tok.nextToken());
    }
    return (String[]) tmpList.toArray(new String[tmpList.size()]);
  }

  /**
   * <p>
   * Recursive depth first search for all documents with .txt - ending (case-insensitive).
   * </p>
   * <p>
   * The given list is filled with all files with a ".txt" extension that were found in the
   * directory subtree of the argument f.
   * </p>
   * <p>
   * No check for null or existence of f is made here, so keep it private.
   * </p>
   * 
   * @param f
   *          The current directory or file (if we visit a leaf).
   */
  private void processRecursive(File f) throws Exception {
    if (f == null) {
      throw new IllegalArgumentException("File argument is null!");
    }
    if (!f.exists()) {
      throw new IllegalArgumentException(f.getAbsolutePath() + " does not exist.");
    }
    if (f.isDirectory()) {
      File[] childs = f.listFiles();
      for (int i = childs.length - 1; i >= 0; i--) {
        processRecursive(childs[i]);
      }
    } else if (this.extensionFilter.accept(f)) {
      this.process(f);
    }
  }

  public final void process() throws Exception {
    if (this.printCharsets) {
      this.printCharsets();
    } else {
      this.verifyFiles();
      this.describe();
      this.processRecursive(this.collectionRoot);
    }
    System.out.println("No exceptional program flow occured!");
  }

  /**
   * All three Files are validated if null, existant and the right type (directory vs. file).
   * 
   * @throws Exception
   *           Sth. does not seem to be valid.
   */
  protected void verifyFiles() throws IllegalArgumentException {
    StringBuffer msg = new StringBuffer();
    /*
     * Manual copy and paste for two file members. Not beautiful but formal correctness (all cases);
     */

    // collectionRoot:
    if (this.collectionRoot == null) {
      msg.append("-> Collection root directory is null!\n");
    } else {
      if (!this.collectionRoot.exists()) {
        msg.append("-> Collection root directory:\"");
        msg.append(this.collectionRoot.getAbsolutePath());
        msg.append("\" does not exist!\n");
      }
    }
    if (this.outputDir == null) {
      msg.append("-> Output directory is null!\n");
    } else {
      this.outputDir.mkdirs();
      if (!this.outputDir.isDirectory()) {
        msg.append("-> Output directory has to be a directory, no File!\n");
      }
    }
    // Uhmmkeh or not:
    if (msg.length() > 0) {
      throw new IllegalArgumentException(msg.toString());
    } else {
      System.out.println("All parameters are valid.");
    }
  }

  /**
   * @param files
   */
  private void process(File document) throws Exception {
    Charset charset = null;
    try {
      Thread.sleep(this.wait);
    } catch (InterruptedException e) {
      // nop
    }
    Map.Entry filenameFinder;
    String prefix; // the path between this.collectionRoot and the file.
    File target;

    filenameFinder = FileUtil.cutDirectoryInformation(document.getAbsolutePath());
    prefix = document.getAbsolutePath();
    int stop = prefix.lastIndexOf(fileseparator);
    int start = this.collectionRoot.getAbsolutePath().length();
    if (start > stop) {
      prefix = "";
    } else {
      prefix = prefix.substring(this.collectionRoot.getAbsolutePath().length(), stop + 1);
    }
    if (this.verbose) {
      System.out.println("Processing document: " + prefix + "/" + filenameFinder.getValue());
    }
    charset = this.detector.detectCodepage(document.toURL());

    if ((charset == null) || (charset == UnknownCharset.getInstance())) {
      if (this.verbose) {
        System.out.println("  Charset not detected.");
      }
      if (!this.moveUnknown) {
        if (this.verbose) {
          System.out.println("  Dropping document.");
        }
        return;
      } else {
        // fake charset for name construction:
        charset = UnknownCharset.getInstance();
      }
    }

    if ((this.targetCodepage != null) && (charset != null)
        && (UnknownCharset.getInstance() != charset)) {

      // transform it:
      if (prefix.length() > 0) {
        target = new File(this.outputDir.getAbsolutePath() + "/" + this.targetCodepage.name() + "/"
            + prefix + "/");
      } else {
        target = new File(this.outputDir.getAbsolutePath() + "/" + this.targetCodepage.name() + "/");
      }
      if (target.mkdirs()) {
        if (this.verbose) {
          System.out.println("  Created directory : " + target.getAbsolutePath());
        }
      }
      target = new File(target.getAbsolutePath() + "/" + filenameFinder.getValue());
      if (this.verbose) {
        System.out.println("  Moving to \"" + target.getAbsolutePath() + "\".");
      }
      if (target.exists() && target.length() == document.length()) {
        if (this.verbose) {
          System.out.println("  File already exists and has same size. Skipping move.");
        }
      } else {
        target.createNewFile();
        Reader in = new BufferedReader(
            new InputStreamReader(new FileInputStream(document), charset));
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target),
            this.targetCodepage));

        // da flow
        int toRead = transcodeBuffer.length;
        int len;
        while ((len = in.read(transcodeBuffer, 0, toRead)) != -1) {
          out.write(transcodeBuffer, 0, len);
        }
        in.close();
        out.close();
      }
    } else {
      if (this.targetCodepage != null) {
        System.out.println("Skipping transformation of document " + document.getAbsolutePath()
            + " because it's charset could not be detected.");
      }
      if (prefix.length() > 0) {
        target = new File(this.outputDir.getAbsolutePath() + "/" + charset.name().toLowerCase()
            + "/" + prefix + "/");
      } else {
        target = new File(this.outputDir.getAbsolutePath() + "/" + charset.name().toLowerCase()
            + "/");
      }
      if (target.mkdirs()) {
        if (this.verbose) {
          System.out.println("Created directory : " + target.getAbsolutePath());
        }
      }
      target = new File(target.getAbsolutePath() + "/" + filenameFinder.getValue());
      if (this.verbose) {

        System.out.println("  Moving to \"" + target.getAbsolutePath() + "\".");
      }
      this.rawCopy(document, target);
    }
  }

  private void rawCopy(File from, File to) throws IOException {
    if (to.exists()) {
      if (from.length() == to.length()) {
        return;
      }
    } else {
      to.createNewFile();
    }
    /*
     * Target existed and had the same length : skip: Target existed and had a different length:
     * overwrite target. Target did not exist: Create it first before writing.
     */
    InputStream in = new BufferedInputStream(new FileInputStream(from));
    OutputStream out = new BufferedOutputStream(new FileOutputStream(to));

    // da flow
    int toRead = rawtransportBuffer.length;
    int len;
    while ((len = in.read(rawtransportBuffer, 0, toRead)) != -1) {
      out.write(rawtransportBuffer, 0, len);
    }
    in.close();
    out.close();
  }

  protected void describe() {
    StringBuffer msg = new StringBuffer();
    msg.append("Setup:\n");
    msg.append("  Collection-Root        : ");
    msg.append(this.collectionRoot.getAbsolutePath());
    msg.append("\n");
    msg.append("  Output-Dir             : ");
    msg.append(this.outputDir.getAbsolutePath());
    msg.append("\n");
    msg.append("  Move unknown           : ");
    msg.append(this.moveUnknown);
    msg.append("\n");
    msg.append("  verbose                : ");
    msg.append(this.verbose);
    msg.append("\n");
    msg.append("  wait                   : ");
    msg.append(this.wait);
    msg.append("\n");
    if (this.targetCodepage != null) {
      msg.append("  transform to codepage  : ");
      msg.append(this.targetCodepage.name());
      msg.append("\n");
    }
    msg.append("  detection algorithm    : ");
    msg.append("\n");
    msg.append(this.detector.toString());
    System.out.println(msg.toString());
  }

  /**
   * Prints out the usage of the command line interface.
   * <p>
   */
  protected void usage() {
    StringBuffer tmp = new StringBuffer();

    tmp.append("usage: java -cp jargs-1.0.jar")
        .append(File.separatorChar)
        .append("cpdetector_1.0.9.jar")
        .append(File.pathSeparatorChar)
        .append("antlr-2.7.4.jar")
        .append(File.pathSeparatorChar)
        .append(
            "chardet.jar info.monitorenter.cpdetector.CodepageProcessor -r <testdocumentdir> -o <testoutputdir> [options]");
    tmp.append("\n");
    tmp.append("options: \n");
    tmp.append("\n  Optional:\n");
    tmp.append("  -c              : Only print available charsets on this system.\n");
    tmp.append("  -e <extensions> : A comma- or semicolon- separated string for document extensions like \"-e txt,dat\" (without dot or space!).\n");
    tmp.append("  -m              : Move files with unknown charset to directory \"unknown\".\n");
    tmp.append("  -v              : Verbose output.\n");
    tmp.append("  -w <int>        : Wait <int> seconds before trying next document (good, if you want to work on the very same machine).\n");
    tmp.append("  -t <charset>    : Try to transform the document to given charset (codepage) name. \n");
    tmp.append("                    This is only possible for documents that are detected to have a  \n");
    tmp.append("                    codepage that is supported by the current java VM. If not possible \n");
    tmp.append("                    sorting will be done as normal. \n");
    tmp.append("  -d              : Semicolon-separated list of fully qualified classnames. \n");
    tmp.append("                    These classes will be casted to ICodepageDetector instances \n");
    tmp.append("                    and used in the order specified.\n");
    tmp.append("                    If this argument is ommited, a HTMLCodepageDetector followed by .\n");
    tmp.append("                    a JChardetFacade is used by default.\n");
    tmp.append("  Mandatory (if no -c option given) :\n");
    tmp.append("  -r            : Root directory containing the collection (recursive).\n");
    tmp.append("  -o            : Output directory containing the sorted collection.\n");
    System.out.print(tmp.toString());
  }

  void loadCodepages() {
    SortedMap<String, Charset> charSets = Charset.availableCharsets();
    Iterator<Map.Entry<String, Charset>> csIt = charSets.entrySet().iterator();
    Map.Entry<String, Charset> entry;
    Iterator<String> aliasIt;
    Set<String> aliases;
    Charset cs;
    if (this.verbose) {
      System.out.println("Loading system codepages...");
    }
    this.parseCodepages = new Charset[charSets.size()];
    int index = 0;
    while (csIt.hasNext()) {
      entry = csIt.next();
      cs = (Charset) entry.getValue();
      if (this.verbose) {
        System.out.println("Charset: " + cs.name());
        aliases = cs.aliases();
        System.out.println("  Aliases: ");
        aliasIt = aliases.iterator();
        while (aliasIt.hasNext()) {
          System.out.println("    " + aliasIt.next().toString());
        }
      }
      this.parseCodepages[index] = cs;
      index++;
    }
  }

  /**
   * Main hook. 
   * <p> 
   * 
   * @param args see {@link #usage()}
   * 
   * @throws Exception if program terminated unsuccessful.
   */
  public static void main(String[] args) throws Exception {
    CodepageProcessor sorter = new CodepageProcessor();
      sorter.parseArgs(args);
      sorter.process();
  }
}
