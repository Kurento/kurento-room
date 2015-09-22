# Makefile for Sphinx documentation
#

# Versions
# IMPORTANT: Notice that DOC_VERSION should be the same as variable "release" in conf.py,
# so when this variable is updated, it should be also updated in conf.py

DOC_VERSION = 6.1.1-dev
KMS_VERSION = 6.1.0
CLIENT_JAVA_VERSION = 6.1.0
CLIENT_JS_VERSION = 6.1.0
UTILS_JS_VERSION = 6.1.0

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
PAPER         =
BUILDDIR      = ../../../target/site

# Internal variables.
PAPEROPT_a4     = -D latex_paper_size=a4
PAPEROPT_letter = -D latex_paper_size=letter
ALLSPHINXOPTS   = -d $(BUILDDIR)/doctrees $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) .
# the i18n builder cannot share the environment and doctrees with the others
I18NSPHINXOPTS  = $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) src/site/sphinx

DOCLINT = $(shell ./doclint.sh)

.PHONY: help clean html dirhtml singlehtml pickle json htmlhelp langdoc qthelp devhelp epub latex latexpdf text man changes linkcheck doctest gettext dist

help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  langdoc    to make javadocs and jsdocs of the Kurento Clients"
	@echo "  html       to make standalone HTML files"
	@echo "  dist       to make langdoc html epub latexpdf and then copy"
	@echo "             Kurento.{pdf,epub} in $(BUILDDIR)/html and make a tgz"
	@echo "             as kurento-docs-$(DOC_VERSION).tgz"
	@echo "  dirhtml    to make HTML files named index.html in directories"
	@echo "  singlehtml to make a single large HTML file"
	@echo "  pickle     to make pickle files"
	@echo "  json       to make JSON files"
	@echo "  htmlhelp   to make HTML files and a HTML help project"
	@echo "  qthelp     to make HTML files and a qthelp project"
	@echo "  devhelp    to make HTML files and a Devhelp project"
	@echo "  epub       to make an epub"
	@echo "  latex      to make LaTeX files, you can set PAPER=a4 or PAPER=letter"
	@echo "  latexpdf   to make LaTeX files and run them through pdflatex"
	@echo "  text       to make text files"
	@echo "  man        to make manual pages"
	@echo "  texinfo    to make Texinfo files"
	@echo "  info       to make Texinfo files and run them through makeinfo"
	@echo "  gettext    to make PO message catalogs"
	@echo "  changes    to make an overview of all changed/added/deprecated items"
	@echo "  linkcheck  to check all external links for integrity"
	@echo "  doctest    to run all doctests embedded in the documentation (if enabled)"

clean:
	-rm -rf $(BUILDDIR)/*
	for p in $(APIS); do rm -rf source/$$p/com; done
	-rm -rf source/langdocs

html:
	$(SPHINXBUILD) -b html $(ALLSPHINXOPTS) $(BUILDDIR)/html
	find $(BUILDDIR)/html -name "*.html" -exec sed -i -e "s@|DOC_VERSION|@$(DOC_VERSION)@" {} \;
	find $(BUILDDIR)/html -name "*.html" -exec sed -i -e "s@|CLIENT_JAVA_VERSION|@$(CLIENT_JAVA_VERSION)@" {} \;
	find $(BUILDDIR)/html -name "*.html" -exec sed -i -e "s@|CLIENT_JS_VERSION|@$(CLIENT_JS_VERSION)@" {} \;
	find $(BUILDDIR)/html -name "*.html" -exec sed -i -e "s@|UTILS_JS_VERSION|@$(UTILS_JS_VERSION)@" {} \;
	./fixlinks.sh
	@echo
	@echo "Build finished. The HTML pages are in $(BUILDDIR)/html."

dirhtml:
	$(SPHINXBUILD) -b dirhtml $(ALLSPHINXOPTS) $(BUILDDIR)/dirhtml
	@echo
	@echo "Build finished. The HTML pages are in $(BUILDDIR)/dirhtml."

singlehtml:
	$(SPHINXBUILD) -b singlehtml $(ALLSPHINXOPTS) $(BUILDDIR)/singlehtml
	@echo
	@echo "Build finished. The HTML page is in $(BUILDDIR)/singlehtml."

pickle:
	$(SPHINXBUILD) -b pickle $(ALLSPHINXOPTS) $(BUILDDIR)/pickle
	@echo
	@echo "Build finished; now you can process the pickle files."

json:
	$(SPHINXBUILD) -b json $(ALLSPHINXOPTS) $(BUILDDIR)/json
	@echo
	@echo "Build finished; now you can process the JSON files."

htmlhelp:
	$(SPHINXBUILD) -b htmlhelp $(ALLSPHINXOPTS) $(BUILDDIR)/htmlhelp
	@echo
	@echo "Build finished; now you can run HTML Help Workshop with the" \
	      ".hhp project file in $(BUILDDIR)/htmlhelp."

langdoc:
	  @echo "Doclint arg for javadoc (if java version >= 1.8): $(DOCLINT)"
	  
	  mkdir -p $(BUILDDIR)/langdoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-room && rm -rf $(BUILDDIR)/langdoc/kurento-java
	  mkdir -p $(BUILDDIR)/html/langdoc/javadoc

	  # kurento-room-sdk javadoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-room-sdk
	  rsync -av --exclude 'target' ../../../../kurento-room-sdk/* $(BUILDDIR)/langdoc/kurento-room-sdk
	  cd $(BUILDDIR)/langdoc/kurento-room-sdk && mvn clean package -DskipTests
	  
	  # kurento-client javadoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-client
	  cd  $(BUILDDIR)/langdoc && git clone https://github.com/Kurento/kurento-java.git && \
	    cd kurento-java && git checkout kurento-java-$(CLIENT_JAVA_VERSION) || \
	    git checkout $(CLIENT_JAVA_VERSION) || echo "Using master branch"
	  mv $(BUILDDIR)/langdoc/kurento-java/kurento-client $(BUILDDIR)/langdoc
	  cd $(BUILDDIR)/langdoc/kurento-client && mvn clean package -DskipTests
	  rsync -av $(BUILDDIR)/langdoc/kurento-client/target/generated-sources/kmd/* $(BUILDDIR)/langdoc/kurento-client/src/main/java/
	   
	  javadoc -Xdoclint:none -windowtitle "Kurento Room SDK Javadoc" \
	    -d $(BUILDDIR)/html/langdoc/javadoc \
	    -sourcepath $(BUILDDIR)/langdoc/kurento-room-sdk/src/main/java/:$(BUILDDIR)/langdoc/kurento-client/src/main/java/ \
	    -subpackages org.kurento.room:org.kurento.client \
	    -exclude org.kurento.room.endpoint:org.kurento.room.internal:org.kurento.client.internal

qthelp:
	$(SPHINXBUILD) -b qthelp $(ALLSPHINXOPTS) $(BUILDDIR)/qthelp
	@echo
	@echo "Build finished; now you can run "qcollectiongenerator" with the" \
	      ".qhcp project file in $(BUILDDIR)/qthelp, like this:"
	@echo "# qcollectiongenerator $(BUILDDIR)/qthelp/Kurento.qhcp"
	@echo "To view the help file:"
	@echo "# assistant -collectionFile $(BUILDDIR)/qthelp/Kurento.qhc"

devhelp:
	$(SPHINXBUILD) -b devhelp $(ALLSPHINXOPTS) $(BUILDDIR)/devhelp
	@echo
	@echo "Build finished."
	@echo "To view the help file:"
	@echo "# mkdir -p $$HOME/.local/share/devhelp/Kurento"
	@echo "# ln -s $(BUILDDIR)/devhelp $$HOME/.local/share/devhelp/Kurento"
	@echo "# devhelp"

epub:
	$(SPHINXBUILD) -b epub $(ALLSPHINXOPTS) $(BUILDDIR)/epub
	find $(BUILDDIR)/epub -name "*.html" -exec sed -i -e "s@|DOC_VERSION|@$(DOC_VERSION)@" {} \;
	find $(BUILDDIR)/epub -name "*.html" -exec sed -i -e "s@|CLIENT_JAVA_VERSION|@$(CLIENT_JAVA_VERSION)@" {} \;
	find $(BUILDDIR)/epub -name "*.html" -exec sed -i -e "s@|CLIENT_JS_VERSION|@$(CLIENT_JS_VERSION)@" {} \;
	find $(BUILDDIR)/epub -name "*.html" -exec sed -i -e "s@|UTILS_JS_VERSION|@$(UTILS_JS_VERSION)@" {} \;
	$(SPHINXBUILD) -b epub $(ALLSPHINXOPTS) $(BUILDDIR)/epub
	@echo
	@echo "Build finished. The epub file is in $(BUILDDIR)/epub."

latex:
	$(SPHINXBUILD) -b latex $(ALLSPHINXOPTS) $(BUILDDIR)/latex
	@echo
	@echo "Build finished; the LaTeX files are in $(BUILDDIR)/latex."
	@echo "Run \`make' in that directory to run these through (pdf)latex" \
	      "(use \`make latexpdf' here to do that automatically)."

latexpdf:
	$(SPHINXBUILD) -b latex $(ALLSPHINXOPTS) $(BUILDDIR)/latex
	@echo "Running LaTeX files through pdflatex..."
	find $(BUILDDIR)/latex -name "*.tex" -exec sed -i -e "s@.textbar..DOC_VERSION.textbar..@$(DOC_VERSION)@" {} \;
	find $(BUILDDIR)/latex -name "*.tex" -exec sed -i -e "s@.textbar..CLIENT_JAVA_VERSION.textbar..@$(CLIENT_JAVA_VERSION)@" {} \;
	find $(BUILDDIR)/latex -name "*.tex" -exec sed -i -e "s@.textbar..CLIENT_JS_VERSION.textbar..@$(CLIENT_JS_VERSION)@" {} \;
	find $(BUILDDIR)/latex -name "*.tex" -exec sed -i -e "s@.textbar..UTILS_JS_VERSION.textbar..@$(UTILS_JS_VERSION)@" {} \;
	$(MAKE) -C $(BUILDDIR)/latex all-pdf
	@echo "pdflatex finished; the PDF files are in $(BUILDDIR)/latex."

text:
	$(SPHINXBUILD) -b text $(ALLSPHINXOPTS) $(BUILDDIR)/text
	@echo
	@echo "Build finished. The text files are in $(BUILDDIR)/text."

man:
	$(SPHINXBUILD) -b man $(ALLSPHINXOPTS) $(BUILDDIR)/man
	@echo
	@echo "Build finished. The manual pages are in $(BUILDDIR)/man."

texinfo:
	$(SPHINXBUILD) -b texinfo $(ALLSPHINXOPTS) $(BUILDDIR)/texinfo
	@echo
	@echo "Build finished. The Texinfo files are in $(BUILDDIR)/texinfo."
	@echo "Run \`make' in that directory to run these through makeinfo" \
	      "(use \`make info' here to do that automatically)."

info:
	$(SPHINXBUILD) -b texinfo $(ALLSPHINXOPTS) $(BUILDDIR)/texinfo
	@echo "Running Texinfo files through makeinfo..."
	make -C $(BUILDDIR)/texinfo info
	@echo "makeinfo finished; the Info files are in $(BUILDDIR)/texinfo."

gettext:
	$(SPHINXBUILD) -b gettext $(I18NSPHINXOPTS) $(BUILDDIR)/locale
	@echo
	@echo "Build finished. The message catalogs are in $(BUILDDIR)/locale."

changes:
	$(SPHINXBUILD) -b changes $(ALLSPHINXOPTS) $(BUILDDIR)/changes
	@echo
	@echo "The overview file is in $(BUILDDIR)/changes."

linkcheck:
	$(SPHINXBUILD) -b linkcheck $(ALLSPHINXOPTS) $(BUILDDIR)/linkcheck
	@echo
	@echo "Link check complete; look for any errors in the above output " \
	      "or in $(BUILDDIR)/linkcheck/output.txt."

doctest:
	$(SPHINXBUILD) -b doctest $(ALLSPHINXOPTS) $(BUILDDIR)/doctest
	@echo "Testing of doctests in the sources finished, look at the " \
	      "results in $(BUILDDIR)/doctest/output.txt."

dist: langdoc html epub latexpdf
	mkdir -p $(BUILDDIR)/dist
	@echo
	@echo "Packaging documentation"
	@echo
	cp $(BUILDDIR)/epub/KurentoRoom.epub $(BUILDDIR)/latex/KurentoRoom.pdf $(BUILDDIR)/html &&\
	tar zcvf $(BUILDDIR)/dist/kurento-room-docs-$(DOC_VERSION).tgz -C $(BUILDDIR)/html .