#Tests de carga y de rendimiento para la consulta
Test definidos con Locust que ejecutan un usuario que hace distintos tipos
de consultas. Para ejecutar Locust lanzar el siguiente comando en el directorio actual

```
docker run -p 8089:8089 -v $PWD:/mnt/locust locustio/locust -f /mnt/locust/locustfile.py
```
