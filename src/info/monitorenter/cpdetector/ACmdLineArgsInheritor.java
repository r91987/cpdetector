/*
 *  ACmdLineArgsInheritor of project cpdetector, base class for inheritance of command line arguments.
 *  Copyright ${year} (C) Achim Westermann, created on 11.04.2004.
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
package info.monitorenter.cpdetector;

import jargs.gnu.CmdLineParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for inheritance of command line arguments.<p>
 * 
 * @author <a href="mailto:awester@de.ibm.com">Achim Westermann </a>
 * 
 */
public abstract class ACmdLineArgsInheritor {

    private CmdLineParser cmdLineParser;

    /**
     * All {@link jargs.gnu.CommandLineParser.Option}s.
     * <p>
     * Subclasses may specify more options in their constructor by invoking the protected method
     * #addOption(String,CmdLineParser.Option).
     * <p>
     */
    private Map<String,CmdLineParser.Option> cmdLineOptions;

    /**
     * 
     */
    public ACmdLineArgsInheritor() {
        this.cmdLineOptions = new HashMap<String,CmdLineParser.Option>();
        this.cmdLineParser = new CmdLineParser();
    }

    /**
     * Subclasses have to call from their constructor and implant their desired options. Later on they can retrieve
     * those options by using method <tt>getParsedCmdLineOption(String key)</tt> and do whatever they desire with the
     * value then contained within the option.
     * 
     * @param key
     *            The same key as used for <tt>getParsedCmdLineOption(String key)</tt>
     */
    protected final void addCmdLineOption(String key, CmdLineParser.Option option) {
        if (option == null) {
            throw new IllegalArgumentException(
                    "Specify a valid Option of a type within jargs.gnu.CmdLineParser.Option instead of null!");
        }
        if (this.cmdLineOptions.containsKey(key)) {
            throw new IllegalArgumentException("Ambiguity: Option: " + String.valueOf(key)
                    + " already added.");
        }
        this.cmdLineOptions.put(key, option);
        this.cmdLineParser.addOption(option);
    }

    /**
     * Returns the option <b>value </b> of a parsed command line option.
     */
    protected final Object getParsedCmdLineOption(String key) throws IllegalArgumentException {
        Object ret = this.cmdLineOptions.get(key);
        if (key == null) {
            throw new IllegalArgumentException("Option with key: \"" + String.valueOf(key)
                    + "\" has not been set in constructor.");
        }
        return this.cmdLineParser.getOptionValue((CmdLineParser.Option)ret);
    }

    /**
     * <p>
     * This method has to be called initially by the code using this instance in order to configure.
     * </p>
     * <p>
     * Every subclass has to call <code>super.parseArgs(cmdLineArgs)</code> and then retrieve the options needed from
     * the returned CmdLineParser!
     * </p>
     * 
     * @param cmdLineArgs
     */
    public void parseArgs(String[] cmdLineArgs) throws Exception {
      
        this.cmdLineParser.parse(cmdLineArgs);

    }

    protected abstract void usage();

}
