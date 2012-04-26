    public void testGetPackageVersionsForAtom() throws MalformedURLException, IOException {
        URL url = new URL(baseURL, "rest/packages/mortgages/versions");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestProperty("Authorization",
                "Basic " + new Base64().encodeToString(( "admin:admin".getBytes() )));
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();

        //System.out.println(IOUtils.toString(connection.getInputStream()));

        InputStream in = connection.getInputStream();
        Document<Feed> doc = abdera.getParser().parse(in);
        Feed feed = doc.getRoot();

        List<Entry> entries = feed.getEntries();
        for(Entry entry : entries){
            System.out.println("Version title: " +  entry.getTitle());
            System.out.println("Href: " + entry.getLinks().get(0).getHref().getPath());
        }
    } 