////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package io.github.yangziwen.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.util.StringUtils;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.AstTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.Definitions;
import com.puppycrawl.tools.checkstyle.DetailNodeTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.SuppressionsStringPrinter;
import com.puppycrawl.tools.checkstyle.ThreadModeSettings;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.XpathFileGeneratorAstFilter;
import com.puppycrawl.tools.checkstyle.XpathFileGeneratorAuditListener;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.api.RootModule;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import io.github.yangziwen.checkstyle.diff.DiffCalculator;
import io.github.yangziwen.checkstyle.diff.DiffEntryWrapper;
import io.github.yangziwen.checkstyle.filter.DiffLineFilter;

/**
 * Wrapper command line program for the Checker.
 * @noinspection UseOfSystemOutOrSystemErr
 **/
public final class Main {

    /**
     * A key pointing to the error counter
     * message in the "messages.properties" file.
     */
    public static final String ERROR_COUNTER = "Main.errorCounter";
    /**
     * A key pointing to the load properties exception
     * message in the "messages.properties" file.
     */
    public static final String LOAD_PROPERTIES_EXCEPTION = "Main.loadProperties";
    /**
     * A key pointing to the create listener exception
     * message in the "messages.properties" file.
     */
    public static final String CREATE_LISTENER_EXCEPTION = "Main.createListener";
    /** Logger for Main. */
    private static final Log LOG = LogFactory.getLog(Main.class);

    /** Width of CLI help option. */
    private static final int HELP_WIDTH = 100;

    /** Exit code returned when execution finishes with {@link CheckstyleException}. */
    private static final int EXIT_WITH_CHECKSTYLE_EXCEPTION_CODE = -2;

    /** Name for the option 'v'. */
    private static final String OPTION_V_NAME = "v";

    /** Name for the option 'c'. */
    private static final String OPTION_C_NAME = "c";

    /** Name for the option 'f'. */
    private static final String OPTION_F_NAME = "f";

    /** Name for the option 'p'. */
    private static final String OPTION_P_NAME = "p";

    /** Name for the option 'o'. */
    private static final String OPTION_O_NAME = "o";

    /** Name for the option 's'. */
    private static final String OPTION_S_NAME = "s";

    /** Name for the option 't'. */
    private static final String OPTION_T_NAME = "t";

    /** Name for the option '--tree'. */
    private static final String OPTION_TREE_NAME = "tree";

    /** Name for the option 'tabWidth'. */
    private static final String OPTION_TAB_WIDTH_NAME = "tabWidth";

    /** Name for the option '-T'. */
    private static final String OPTION_CAPITAL_T_NAME = "T";

    /** Name for the option '--treeWithComments'. */
    private static final String OPTION_TREE_COMMENT_NAME = "treeWithComments";

    /** Name for the option '-j'. */
    private static final String OPTION_J_NAME = "j";

    /** Name for the option '--javadocTree'. */
    private static final String OPTION_JAVADOC_TREE_NAME = "javadocTree";

    /** Name for the option '-J'. */
    private static final String OPTION_CAPITAL_J_NAME = "J";

    /** Name for the option '--treeWithJavadoc'. */
    private static final String OPTION_TREE_JAVADOC_NAME = "treeWithJavadoc";

    /** Name for the option '-d'. */
    private static final String OPTION_D_NAME = "d";

    /** Name for the option '--debug'. */
    private static final String OPTION_DEBUG_NAME = "debug";

    /** Name for the option 'e'. */
    private static final String OPTION_E_NAME = "e";

    /** Name for the option '--exclude'. */
    private static final String OPTION_EXCLUDE_NAME = "exclude";

    /** Name for the option '--executeIgnoredModules'. */
    private static final String OPTION_EXECUTE_IGNORED_MODULES_NAME = "executeIgnoredModules";

    /** Name for the option 'x'. */
    private static final String OPTION_X_NAME = "x";

    /** Name for the option '--exclude-regexp'. */
    private static final String OPTION_EXCLUDE_REGEXP_NAME = "exclude-regexp";

    /** Name for the option '-C'. */
    private static final String OPTION_CAPITAL_C_NAME = "C";

    /** Name for the option '--checker-threads-number'. */
    private static final String OPTION_CHECKER_THREADS_NUMBER_NAME = "checker-threads-number";

    /** Name for the option '-W'. */
    private static final String OPTION_CAPITAL_W_NAME = "W";

    /** Name for the option '--tree-walker-threads-number'. */
    private static final String OPTION_TREE_WALKER_THREADS_NUMBER_NAME =
        "tree-walker-threads-number";

    /** Name for the option 'gxs'. */
    private static final String OPTION_GXS_NAME = "gxs";

    /** Name for the option 'generate-xpath-suppression'. */
    private static final String OPTION_GENERATE_XPATH_SUPPRESSION_NAME =
            "generate-xpath-suppression";

    /** Name for the option 'gd'. */
    private static final String OPTION_GD_NAME = "gd";

    /** Name for the option 'git-directory'. */
    private static final String OPTION_GIT_DIR_NAME = "git-dir";

    /** Name for the option 'br'. */
    private static final String OPTION_BR_NAME = "br";

    /** Name for the option 'base-rev' */
    private static final String OPTION_GIT_BASE_REV_NAME = "base-rev";

    /** Name for the option 'ic'. */
    private static final String OPTION_IS_NAME = "is";

    /** Name for the option 'include-staged-codes' */
    private static final String OPTION_GIT_INCLUDE_STAGED_CODES_NAME = "include-staged-codes";

    /** Name for 'xml' format. */
    private static final String XML_FORMAT_NAME = "xml";

    /** Name for 'plain' format. */
    private static final String PLAIN_FORMAT_NAME = "plain";

    /** A string value of 1. */
    private static final String ONE_STRING_VALUE = "1";

    /** Default distance between tab stops. */
    private static final String DEFAULT_TAB_WIDTH = "8";

    private static final List<DiffEntryWrapper> DIFF_ENTRY_LIST = new ArrayList<>();


    /** Don't create instance of this class, use {@link #main(String[])} method instead. */
    private Main() {
    }

    /**
     * Loops over the files specified checking them for errors. The exit code
     * is the number of errors found in all the files.
     * @param args the command line arguments.
     * @throws IOException if there is a problem with files access
     * @noinspection CallToPrintStackTrace, CallToSystemExit
     **/
    public static void main(String... args) throws IOException {
        int errorCounter = 0;
        boolean cliViolations = false;
        // provide proper exit code based on results.
        final int exitWithCliViolation = -1;
        int exitStatus = 0;

        try {
            //parse CLI arguments
            final CommandLine commandLine = parseCli(args);

            // show version and exit if it is requested
            if (commandLine.hasOption(OPTION_V_NAME)) {
                System.out.println("Checkstyle version: "
                        + Main.class.getPackage().getImplementationVersion());
                exitStatus = 0;
            }
            else {
                List<File> filesToProcess = Collections.emptyList();

                if (commandLine.hasOption(OPTION_GIT_DIR_NAME)) {
                    filesToProcess = getGitDiffFilesToProcess(getExclusions(commandLine), commandLine);
                    if (CollectionUtils.isEmpty(filesToProcess)) {
                        System.out.println("There is no file need to check");
                        return;
                    }
                } else {
                    filesToProcess = getFilesToProcess(getExclusions(commandLine),
                            commandLine.getArgs());
                }

                // return error if something is wrong in arguments
                final List<String> messages = validateCli(commandLine, filesToProcess);
                cliViolations = !messages.isEmpty();
                if (cliViolations) {
                    exitStatus = exitWithCliViolation;
                    errorCounter = 1;
                    messages.forEach(System.out::println);
                }
                else {
                    errorCounter = runCli(commandLine, filesToProcess);
                    exitStatus = errorCounter;
                }
            }
        }
        catch (ParseException pex) {
            // something wrong with arguments - print error and manual
            cliViolations = true;
            exitStatus = exitWithCliViolation;
            errorCounter = 1;
            System.out.println(pex.getMessage());
            printUsage();
        }
        catch (CheckstyleException ex) {
            exitStatus = EXIT_WITH_CHECKSTYLE_EXCEPTION_CODE;
            errorCounter = 1;
            ex.printStackTrace();
        }
        finally {
            // return exit code base on validation of Checker
            // two ifs exist till https://github.com/hcoles/pitest/issues/377
            if (errorCounter != 0) {
                if (!cliViolations) {
                    final Violation errorCounterMessage = new Violation(1,
                            Definitions.CHECKSTYLE_BUNDLE, ERROR_COUNTER,
                            new String[] {String.valueOf(errorCounter)}, null, Main.class, null);
                    System.out.println(errorCounterMessage.getViolation());
                }
            }
            if (exitStatus != 0) {
                System.exit(exitStatus);
            }
        }
    }

    /**
     * Parses and executes Checkstyle based on passed arguments.
     * @param args
     *        command line parameters
     * @return parsed information about passed parameters
     * @throws ParseException
     *         when passed arguments are not valid
     */
    private static CommandLine parseCli(String... args)
            throws ParseException {
        // parse the parameters
        final CommandLineParser clp = new DefaultParser();
        // always returns not null value
        return clp.parse(buildOptions(), args);
    }

    /**
     * Gets the list of exclusions provided through the command line argument.
     * @param commandLine command line object
     * @return List of exclusion patterns.
     */
    private static List<Pattern> getExclusions(CommandLine commandLine) {
        final List<Pattern> result = new ArrayList<>();

        if (commandLine.hasOption(OPTION_E_NAME)) {
            for (String value : commandLine.getOptionValues(OPTION_E_NAME)) {
                result.add(Pattern.compile("^" + Pattern.quote(new File(value).getAbsolutePath())
                        + "$"));
            }
        }
        if (commandLine.hasOption(OPTION_X_NAME)) {
            for (String value : commandLine.getOptionValues(OPTION_X_NAME)) {
                result.add(Pattern.compile(value));
            }
        }

        return result;
    }

    /**
     * Do validation of Command line options.
     * @param cmdLine command line object
     * @param filesToProcess List of files to process found from the command line.
     * @return list of violations
     */
    // -@cs[CyclomaticComplexity] Breaking apart will damage encapsulation
    private static List<String> validateCli(CommandLine cmdLine, List<File> filesToProcess) {
        final List<String> result = new ArrayList<>();

        if (filesToProcess.isEmpty()) {
            result.add("Files to process must be specified, found 0.");
        }
        // ensure there is no conflicting options
        else if (cmdLine.hasOption(OPTION_T_NAME) || cmdLine.hasOption(OPTION_CAPITAL_T_NAME)
                || cmdLine.hasOption(OPTION_J_NAME) || cmdLine.hasOption(OPTION_CAPITAL_J_NAME)) {
            if (cmdLine.hasOption(OPTION_S_NAME) || cmdLine.hasOption(OPTION_C_NAME)
                    || cmdLine.hasOption(OPTION_P_NAME) || cmdLine.hasOption(OPTION_F_NAME)
                    || cmdLine.hasOption(OPTION_O_NAME)) {
                result.add("Option '-t' cannot be used with other options.");
            }
            else if (filesToProcess.size() > 1) {
                result.add("Printing AST is allowed for only one file.");
            }
        }
        else if (cmdLine.hasOption(OPTION_S_NAME)) {
            if (cmdLine.hasOption(OPTION_C_NAME) || cmdLine.hasOption(OPTION_P_NAME)
                    || cmdLine.hasOption(OPTION_F_NAME) || cmdLine.hasOption(OPTION_O_NAME)) {
                result.add("Option '-s' cannot be used with other options.");
            }
            else if (filesToProcess.size() > 1) {
                result.add("Printing xpath suppressions is allowed for only one file.");
            }
        }
        // ensure a configuration file is specified
        else if (cmdLine.hasOption(OPTION_C_NAME)) {
            final String configLocation = cmdLine.getOptionValue(OPTION_C_NAME);
            try {
                // test location only
                CommonUtil.getUriByFilename(configLocation);
            }
            catch (CheckstyleException ignored) {
                result.add(String.format("Could not find config XML file '%s'.", configLocation));
            }

            // validate optional parameters
            if (cmdLine.hasOption(OPTION_F_NAME)) {
                final String format = cmdLine.getOptionValue(OPTION_F_NAME);
                if (!PLAIN_FORMAT_NAME.equals(format) && !XML_FORMAT_NAME.equals(format)) {
                    result.add(String.format("Invalid output format."
                            + " Found '%s' but expected '%s' or '%s'.",
                            format, PLAIN_FORMAT_NAME, XML_FORMAT_NAME));
                }
            }
            if (cmdLine.hasOption(OPTION_P_NAME)) {
                final String propertiesLocation = cmdLine.getOptionValue(OPTION_P_NAME);
                final File file = new File(propertiesLocation);
                if (!file.exists()) {
                    result.add(String.format("Could not find file '%s'.", propertiesLocation));
                }
            }
            verifyThreadsNumberParameter(cmdLine, result, OPTION_CAPITAL_C_NAME,
                "Checker threads number must be greater than zero",
                "Invalid Checker threads number");
            verifyThreadsNumberParameter(cmdLine, result, OPTION_CAPITAL_W_NAME,
                "TreeWalker threads number must be greater than zero",
                "Invalid TreeWalker threads number");
        }
        else {
            result.add("Must specify a config XML file.");
        }

        return result;
    }

    /**
     * Verifies threads number CLI parameter value.
     * @param cmdLine a command line
     * @param result a resulting list of errors
     * @param cliParameterName a CLI parameter name
     * @param mustBeGreaterThanZeroMessage a message which should be reported
     *                                     if the number of threads is less than or equal to zero
     * @param invalidNumberMessage a message which should be reported if the passed value
     *                             is not a valid number
     */
    private static void verifyThreadsNumberParameter(CommandLine cmdLine, List<String> result,
        String cliParameterName, String mustBeGreaterThanZeroMessage,
        String invalidNumberMessage) {
        if (cmdLine.hasOption(cliParameterName)) {
            final String checkerThreadsNumberStr =
                cmdLine.getOptionValue(cliParameterName);
            try {
                final int checkerThreadsNumber = Integer.parseInt(checkerThreadsNumberStr);
                if (checkerThreadsNumber < 1) {
                    result.add(mustBeGreaterThanZeroMessage);
                }
            } catch (NumberFormatException ignored) {
                result.add(invalidNumberMessage);
            }
        }
    }

    /**
     * Do execution of CheckStyle based on Command line options.
     * @param commandLine command line object
     * @param filesToProcess List of files to process found from the command line.
     * @return number of violations
     * @throws IOException if a file could not be read.
     * @throws CheckstyleException if something happens processing the files.
     */
    private static int runCli(CommandLine commandLine, List<File> filesToProcess)
            throws IOException, CheckstyleException {
        int result = 0;

        // create config helper object
        final CliOptions config = convertCliToPojo(commandLine, filesToProcess);
        if (commandLine.hasOption(OPTION_T_NAME)) {
            // print AST
            final File file = config.files.get(0);
            final String stringAst = AstTreeStringPrinter.printFileAst(file,
                    JavaParser.Options.WITHOUT_COMMENTS);
            System.out.print(stringAst);
        }
        else if (commandLine.hasOption(OPTION_CAPITAL_T_NAME)) {
            final File file = config.files.get(0);
            final String stringAst = AstTreeStringPrinter.printFileAst(file,
                    JavaParser.Options.WITH_COMMENTS);
            System.out.print(stringAst);
        }
        else if (commandLine.hasOption(OPTION_J_NAME)) {
            final File file = config.files.get(0);
            final String stringAst = DetailNodeTreeStringPrinter.printFileAst(file);
            System.out.print(stringAst);
        }
        else if (commandLine.hasOption(OPTION_CAPITAL_J_NAME)) {
            final File file = config.files.get(0);
            final String stringAst = AstTreeStringPrinter.printJavaAndJavadocTree(file);
            System.out.print(stringAst);
        }
        else if (commandLine.hasOption(OPTION_S_NAME)) {
            final File file = config.files.get(0);
            final String suppressionLineColumnNumber = config.suppressionLineColumnNumber;
            final int tabWidth = config.tabWidth;
            final String stringSuppressions =
                    SuppressionsStringPrinter.printSuppressions(file,
                            suppressionLineColumnNumber, tabWidth);
            System.out.print(stringSuppressions);
        }
        else {
            if (commandLine.hasOption(OPTION_D_NAME)) {
                final Logger parentLogger = Logger.getLogger(Main.class.getName()).getParent();
                final ConsoleHandler handler = new ConsoleHandler();
                handler.setLevel(Level.FINEST);
                handler.setFilter(new Filter() {
                    private final String packageName = Main.class.getPackage().getName();

                    @Override
                    public boolean isLoggable(LogRecord record) {
                        return record.getLoggerName().startsWith(packageName);
                    }
                });
                parentLogger.addHandler(handler);
                parentLogger.setLevel(Level.FINEST);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checkstyle debug logging enabled");
                LOG.debug("Running Checkstyle with version: "
                        + Main.class.getPackage().getImplementationVersion());
            }

            // run Checker
            result = runCheckstyle(config);
        }

        return result;
    }

    /**
     * Util method to convert CommandLine type to POJO object.
     * @param cmdLine command line object
     * @param filesToProcess List of files to process found from the command line.
     * @return command line option as POJO object
     */
    private static CliOptions convertCliToPojo(CommandLine cmdLine, List<File> filesToProcess) {
        final CliOptions conf = new CliOptions();
        conf.format = cmdLine.getOptionValue(OPTION_F_NAME);
        if (conf.format == null) {
            conf.format = PLAIN_FORMAT_NAME;
        }
        conf.outputLocation = cmdLine.getOptionValue(OPTION_O_NAME);
        conf.configLocation = cmdLine.getOptionValue(OPTION_C_NAME);
        conf.propertiesLocation = cmdLine.getOptionValue(OPTION_P_NAME);
        conf.suppressionLineColumnNumber = cmdLine.getOptionValue(OPTION_S_NAME);
        conf.files = filesToProcess;
        conf.executeIgnoredModules = cmdLine.hasOption(OPTION_EXECUTE_IGNORED_MODULES_NAME);
        final String checkerThreadsNumber = cmdLine.getOptionValue(
                OPTION_CAPITAL_C_NAME, ONE_STRING_VALUE);
        conf.checkerThreadsNumber = Integer.parseInt(checkerThreadsNumber);
        final String treeWalkerThreadsNumber = cmdLine.getOptionValue(
                OPTION_CAPITAL_W_NAME, ONE_STRING_VALUE);
        conf.treeWalkerThreadsNumber = Integer.parseInt(treeWalkerThreadsNumber);
        final String tabWidth =
                cmdLine.getOptionValue(OPTION_TAB_WIDTH_NAME, DEFAULT_TAB_WIDTH);
        conf.tabWidth = Integer.parseInt(tabWidth);
        conf.generateXpathSuppressionsFile =
                cmdLine.hasOption(OPTION_GENERATE_XPATH_SUPPRESSION_NAME);
        return conf;
    }

    /**
     * Executes required Checkstyle actions based on passed parameters.
     * @param cliOptions
     *        pojo object that contains all options
     * @return number of violations of ERROR level
     * @throws IOException
     *         when output file could not be found
     * @throws CheckstyleException
     *         when properties file could not be loaded
     */
    private static int runCheckstyle(CliOptions cliOptions)
            throws CheckstyleException, IOException {
        // setup the properties
        final Properties props;

        if (cliOptions.propertiesLocation == null) {
            props = System.getProperties();
        }
        else {
            props = loadProperties(new File(cliOptions.propertiesLocation));
        }

        // create a configuration
        final ThreadModeSettings multiThreadModeSettings =
                new ThreadModeSettings(
                        cliOptions.checkerThreadsNumber, cliOptions.treeWalkerThreadsNumber);

        final ConfigurationLoader.IgnoredModulesOptions ignoredModulesOptions;
        if (cliOptions.executeIgnoredModules) {
            ignoredModulesOptions = ConfigurationLoader.IgnoredModulesOptions.EXECUTE;
        }
        else {
            ignoredModulesOptions = ConfigurationLoader.IgnoredModulesOptions.OMIT;
        }

        final Configuration config = ConfigurationLoader.loadConfiguration(
                cliOptions.configLocation, new PropertiesExpander(props),
                ignoredModulesOptions, multiThreadModeSettings);

        // create RootModule object and run it
        final int errorCounter;
        final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
        final RootModule rootModule = getRootModule(config.getName(), moduleClassLoader);

        try {
            final AuditListener listener;
            if (cliOptions.generateXpathSuppressionsFile) {
                // create filter to print generated xpath suppressions file
                final Configuration treeWalkerConfig = getTreeWalkerConfig(config);
                if (treeWalkerConfig != null) {
                    final DefaultConfiguration moduleConfig =
                            new DefaultConfiguration(
                                    XpathFileGeneratorAstFilter.class.getName());
                    moduleConfig.addAttribute(OPTION_TAB_WIDTH_NAME,
                            Integer.toString(cliOptions.tabWidth));
                    ((DefaultConfiguration) treeWalkerConfig).addChild(moduleConfig);
                }

                listener = new XpathFileGeneratorAuditListener(System.out,
                        AbstractAutomaticBean.OutputStreamOptions.NONE);
            }
            else {
                listener = createListener(cliOptions.format,
                        cliOptions.outputLocation);
            }

            rootModule.setModuleClassLoader(moduleClassLoader);
            rootModule.configure(config);
            rootModule.addListener(listener);

            if (CollectionUtils.isNotEmpty(DIFF_ENTRY_LIST) && rootModule instanceof Checker) {
                Checker checker = (Checker) rootModule;
                checker.addFilter(new DiffLineFilter(DIFF_ENTRY_LIST));
            }

            // run RootModule
            errorCounter = rootModule.process(cliOptions.files);
        }
        finally {
            rootModule.destroy();
        }

        return errorCounter;
    }

    /**
     * Returns {@code TreeWalker} module configuration.
     * @param config The configuration object.
     * @return The {@code TreeWalker} module configuration.
     */
    private static Configuration getTreeWalkerConfig(Configuration config) {
        Configuration result = null;

        final Configuration[] children = config.getChildren();
        for (Configuration child : children) {
            if ("TreeWalker".equals(child.getName())) {
                result = child;
                break;
            }
        }
        return result;
    }

    /**
     * Creates a new instance of the root module that will control and run
     * Checkstyle.
     * @param name The name of the module. This will either be a short name that
     *        will have to be found or the complete package name.
     * @param moduleClassLoader Class loader used to load the root module.
     * @return The new instance of the root module.
     * @throws CheckstyleException if no module can be instantiated from name
     */
    private static RootModule getRootModule(String name, ClassLoader moduleClassLoader)
            throws CheckstyleException {
        final ModuleFactory factory = new PackageObjectFactory(
                Checker.class.getPackage().getName(), moduleClassLoader);

        return (RootModule) factory.createModule(name);
    }

    /**
     * Loads properties from a File.
     * @param file
     *        the properties file
     * @return the properties in file
     * @throws CheckstyleException
     *         when could not load properties file
     */
    private static Properties loadProperties(File file)
            throws CheckstyleException {
        final Properties properties = new Properties();

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            properties.load(stream);
        }
        catch (final IOException ex) {
            final Violation loadPropertiesExceptionMessage = new Violation(1,
                    Definitions.CHECKSTYLE_BUNDLE, LOAD_PROPERTIES_EXCEPTION,
                    new String[] {file.getAbsolutePath()}, null, Main.class, null);
            throw new CheckstyleException(loadPropertiesExceptionMessage.getViolation(), ex);
        }

        return properties;
    }

    /**
     * This method creates in AuditListener an open stream for validation data, it must be closed by
     * {@link RootModule} (default implementation is {@link Checker}) by calling
     * {@link AuditListener#auditFinished(AuditEvent)}.
     * @param format format of the audit listener
     * @param outputLocation the location of output
     * @return a fresh new {@code AuditListener}
     * @exception IOException when provided output location is not found
     */
    private static AuditListener createListener(String format, String outputLocation)
            throws IOException {
        final AuditListener listener;
        if (XML_FORMAT_NAME.equals(format)) {
            final OutputStream out = getOutputStream(outputLocation);
            final AutomaticBean.OutputStreamOptions closeOutputStreamOption =
                    getOutputStreamOptions(outputLocation);
            listener = new XMLLogger(out, closeOutputStreamOption);
        }
        else if (PLAIN_FORMAT_NAME.equals(format)) {
            final OutputStream out = getOutputStream(outputLocation);
            final AutomaticBean.OutputStreamOptions closeOutputStreamOption =
                    getOutputStreamOptions(outputLocation);
            listener = new DefaultLogger(out, closeOutputStreamOption);
        }
        else {
            final Violation outputFormatExceptionMessage = new Violation(1,
                    Definitions.CHECKSTYLE_BUNDLE, CREATE_LISTENER_EXCEPTION,
                    new String[] {format, PLAIN_FORMAT_NAME, XML_FORMAT_NAME}, null,
                    Main.class, null);
            throw new IllegalStateException(outputFormatExceptionMessage.getViolation());
        }

        return listener;
    }

    /**
     * Create output stream or return System.out
     * @param outputLocation output location
     * @return output stream
     * @throws IOException might happen
     */
    @SuppressWarnings("resource")
    private static OutputStream getOutputStream(String outputLocation) throws IOException {
        final OutputStream result;
        if (outputLocation == null) {
            result = System.out;
        }
        else {
            result = Files.newOutputStream(Paths.get(outputLocation));
        }
        return result;
    }

    /**
     * Create {@link AutomaticBean.OutputStreamOptions} for the given location.
     * @param outputLocation output location
     * @return output stream options
     */
    private static AutomaticBean.OutputStreamOptions getOutputStreamOptions(String outputLocation) {
        final AutomaticBean.OutputStreamOptions result;
        if (outputLocation == null) {
            result = AutomaticBean.OutputStreamOptions.NONE;
        }
        else {
            result = AutomaticBean.OutputStreamOptions.CLOSE;
        }
        return result;
    }

    /**
     * Determines the files to process.
     * @param patternsToExclude The list of directory patterns to exclude from searching.
     * @param filesToProcess
     *        arguments that were not processed yet but shall be
     * @return list of files to process
     */
    private static List<File> getFilesToProcess(List<Pattern> patternsToExclude,
            String... filesToProcess) {
        final List<File> files = new LinkedList<>();
        for (String element : filesToProcess) {
            files.addAll(listFiles(new File(element), patternsToExclude));
        }

        return files;
    }

    private static List<File> getGitDiffFilesToProcess(List<Pattern> patternsToExclude, CommandLine commandLine) {
        String gitDirPath = commandLine.getOptionValue(OPTION_GIT_DIR_NAME);
        File repoDir = new File(gitDirPath);
        if (!repoDir.isDirectory()) {
            System.out.println("git directory " + gitDirPath + " is not a directory!");
            System.exit(1);
        }
        String oldRev = commandLine.getOptionValue(OPTION_GIT_BASE_REV_NAME);
        boolean includeStagedCodes = commandLine.hasOption(OPTION_GIT_INCLUDE_STAGED_CODES_NAME);
        if (StringUtils.isEmptyOrNull(oldRev)) {
            oldRev = includeStagedCodes ? "HEAD" : "HEAD~";
        }
        String newRev = "HEAD";
        DiffCalculator calculator = DiffCalculator.builder().diffAlgorithm(new HistogramDiff()).build();
        try {
            List<DiffEntryWrapper> diffEntryList = calculator.calculateDiff(repoDir, oldRev, newRev, includeStagedCodes)
                    .stream()
                    .filter(diffEntry -> !diffEntry.isDeleted())
                    .filter(diffEntry -> patternsToExclude.stream()
                            .noneMatch(p -> p.matcher(diffEntry.getNewPath()).matches()))
                    .collect(Collectors.toList());

            DIFF_ENTRY_LIST.addAll(diffEntryList);

            return diffEntryList.stream()
                    .map(DiffEntryWrapper::getNewFile)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error happened when calculate git diff");
            return Collections.emptyList();
        }
    }

    /**
     * Traverses a specified node looking for files to check. Found files are added to a specified
     * list. Subdirectories are also traversed.
     * @param node
     *        the node to process
     * @param patternsToExclude The list of directory patterns to exclude from searching.
     * @return found files
     */
    private static List<File> listFiles(File node, List<Pattern> patternsToExclude) {
        // could be replaced with org.apache.commons.io.FileUtils.list() method
        // if only we add commons-io library
        final List<File> result = new LinkedList<>();

        if (node.canRead()) {
            if (node.isDirectory()) {
                if (!isDirectoryExcluded(node.getAbsolutePath(), patternsToExclude)) {
                    final File[] files = node.listFiles();
                    // listFiles() can return null, so we need to check it
                    if (files != null) {
                        for (File element : files) {
                            result.addAll(listFiles(element, patternsToExclude));
                        }
                    }
                }
            }
            else if (node.isFile()) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Checks if a directory {@code path} should be excluded based on if it matches one of the
     * patterns supplied.
     * @param path The path of the directory to check
     * @param patternsToExclude The list of directory patterns to exclude from searching.
     * @return True if the directory matches one of the patterns.
     */
    private static boolean isDirectoryExcluded(String path, List<Pattern> patternsToExclude) {
        boolean result = false;

        for (Pattern pattern : patternsToExclude) {
            if (pattern.matcher(path).find()) {
                result = true;
                break;
            }
        }

        return result;
    }

    /** Prints the usage information. **/
    private static void printUsage() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(HELP_WIDTH);
        formatter.printHelp(String.format("java %s [options] -c <config.xml> file...",
                Main.class.getName()), buildOptions());
    }

    /**
     * Builds and returns list of parameters supported by cli Checkstyle.
     * @return available options
     */
    private static Options buildOptions() {
        final Options options = new Options();
        options.addOption(OPTION_C_NAME, true, "Sets the check configuration file to use.");
        options.addOption(OPTION_O_NAME, true, "Sets the output file. Defaults to stdout");
        options.addOption(OPTION_P_NAME, true, "Loads the properties file");
        options.addOption(OPTION_S_NAME, true,
                "Print xpath suppressions at the file's line and column position. "
                        + "Argument is the line and column number (separated by a : ) in the file "
                        + "that the suppression should be generated for");
        options.addOption(OPTION_TAB_WIDTH_NAME, true,
                String.format("Sets the length of the tab character. Used only with \"-s\" option. "
                        + "Default value is %s",
                        DEFAULT_TAB_WIDTH));
        options.addOption(OPTION_GXS_NAME, OPTION_GENERATE_XPATH_SUPPRESSION_NAME, false,
                "Generates to output a suppression.xml to use to suppress all violations"
                        + " from user's config");
        options.addOption(OPTION_F_NAME, true, String.format(
                "Sets the output format. (%s|%s). Defaults to %s",
                PLAIN_FORMAT_NAME, XML_FORMAT_NAME, PLAIN_FORMAT_NAME));
        options.addOption(OPTION_V_NAME, false, "Print product version and exit");
        options.addOption(OPTION_T_NAME, OPTION_TREE_NAME, false,
                "Print Abstract Syntax Tree(AST) of the file");
        options.addOption(OPTION_CAPITAL_T_NAME, OPTION_TREE_COMMENT_NAME, false,
                "Print Abstract Syntax Tree(AST) of the file including comments");
        options.addOption(OPTION_J_NAME, OPTION_JAVADOC_TREE_NAME, false,
                "Print Parse tree of the Javadoc comment");
        options.addOption(OPTION_CAPITAL_J_NAME, OPTION_TREE_JAVADOC_NAME, false,
                "Print full Abstract Syntax Tree of the file");
        options.addOption(OPTION_D_NAME, OPTION_DEBUG_NAME, false,
                "Print all debug logging of CheckStyle utility");
        options.addOption(OPTION_E_NAME, OPTION_EXCLUDE_NAME, true,
                "Directory path to exclude from CheckStyle");
        options.addOption(OPTION_X_NAME, OPTION_EXCLUDE_REGEXP_NAME, true,
                "Regular expression of directory to exclude from CheckStyle");
        options.addOption(OPTION_EXECUTE_IGNORED_MODULES_NAME, false,
                "Allows ignored modules to be run.");
        options.addOption(OPTION_CAPITAL_C_NAME, OPTION_CHECKER_THREADS_NUMBER_NAME, true,
                "(experimental) The number of Checker threads (must be greater than zero)");
        options.addOption(OPTION_CAPITAL_W_NAME, OPTION_TREE_WALKER_THREADS_NUMBER_NAME, true,
                "(experimental) The number of TreeWalker threads (must be greater than zero)");
        options.addOption(OPTION_GD_NAME, OPTION_GIT_DIR_NAME, true, "The git directory");
        options.addOption(OPTION_BR_NAME, OPTION_GIT_BASE_REV_NAME, true,
                "The git base revision, will proccess the changed files between this revision and HEAD");
        options.addOption(OPTION_IS_NAME, OPTION_GIT_INCLUDE_STAGED_CODES_NAME, false,
                "Whether to include indexed codes when calculating diffs");
        return options;
    }

    /** Helper structure to clear show what is required for Checker to run. **/
    private static class CliOptions {

        /** Properties file location. */
        private String propertiesLocation;
        /** Config file location. */
        private String configLocation;
        /** Output format. */
        private String format;
        /** Output file location. */
        private String outputLocation;
        /** List of file to validate. */
        private List<File> files;
        /** Switch whether to execute ignored modules or not. */
        private boolean executeIgnoredModules;
        /** The checker threads number. */
        private int checkerThreadsNumber;
        /** The tree walker threads number. */
        private int treeWalkerThreadsNumber;
        /** LineNo and columnNo for the suppression. */
        private String suppressionLineColumnNumber;
        /** Tab character length. */
        private int tabWidth;
        /** Switch whether to generate suppressions file or not. */
        private boolean generateXpathSuppressionsFile;

    }

}
