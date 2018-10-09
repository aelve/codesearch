-- start database --
docker run -d -p 5432:5432 -v <your_folder_psql>:/var/lib/postgresql -e POSTGRES_DB='sourcesdb' \ 
-e POSTGRES_USER='postgres' \
-e POSTGRES_PASSWORD='postgres' \
--name postgres postgres

-- start core
docker run -d -v <your_folder_for_data>:/data -v <your_folder_for_index_npm>:/index/npm --link postgres:postgres --name core core make download-haskell 

-- start server
docker run -p 9000:80 -d -v <your_folder_for_data>:/data --link postgres:postgres --name web server

* <your_folder_for_data> from start core and start server should be same
