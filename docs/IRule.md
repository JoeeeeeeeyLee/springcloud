# IRule

核心`choose()`方法，选择一个服务实例

### AbstractLoadBalancerRule

AbstractLoadBalancerRule是一个抽象类，里边主要定义了一个ILoadBalancer，就是我们上文所说的负载均衡器，负载均衡器的功能我们在上文已经说的很详细了，这里就不再赘述，这里定义它的目的主要是辅助负责均衡策略选取合适的服务端实例。

### RandomRule

看名字就知道，这种负载均衡策略就是随机选择一个服务实例，看源码我们知道，在RandomRule的无参构造方法中初始化了一个Random对象，然后在它重写的choose方法又调用了`choose(ILoadBalancer lb, Object key)`这个重载的choose方法，在这个重载的choose方法中，每次利用random对象生成一个不大于服务实例总数的随机数，并将该数作为下标所以获取一个服务实例。

### RoundRobinRule

RoundRobinRule这种负载均衡策略叫做线性负载均衡策略，也就是我们在上文所说的BaseLoadBalancer负载均衡器中默认采用的负载均衡策略。这个类的`choose(ILoadBalancer lb, Object key)`函数整体逻辑是这样的：开启一个计数器count，在while循环中遍历服务清单，获取清单之前先通过incrementAndGetModulo方法获取一个下标，这个下标是一个不断自增长的数先加1然后和服务清单总数取模之后获取到的（所以这个下标从来不会越界），拿着下标再去服务清单列表中取服务，每次循环计数器都会加1，如果连续10次都没有取到服务，则会报一个警告`No available alive servers after 10 tries from load balancer: XXXX`。

### RetryRule

看名字就知道这种负载均衡策略带有重试功能。首先RetryRule中又定义了一个subRule，它的实现类是RoundRobinRule，然后在RetryRule的`choose(ILoadBalancer lb, Object key)`方法中，每次还是采用RoundRobinRule中的choose规则来选择一个服务实例，如果选到的实例正常就返回，如果选择的服务实例为null或者已经失效，则在失效时间deadline之前不断的进行重试（重试时获取服务的策略还是RoundRobinRule中定义的策略），如果超过了deadline还是没取到则会返回一个null。

### WeightedResponseTimeRule

WeightedResponseTimeRule是RoundRobinRule的一个子类，在WeightedResponseTimeRule中对RoundRobinRule的功能进行了扩展，WeightedResponseTimeRule中会根据每一个实例的运行情况来给计算出该实例的一个权重，然后在挑选实例的时候则根据权重进行挑选，这样能够实现更优的实例调用。WeightedResponseTimeRule中有一个名叫DynamicServerWeightTask的定时任务，默认情况下每隔30秒会计算一次各个服务实例的权重，权重的计算规则也很简单，如果一个服务的平均响应时间越短则权重越大，那么该服务实例被选中执行任务的概率也就越大。

### ClientConfigEnabledRoundRobinRule

ClientConfigEnabledRoundRobinRule选择策略的实现很简单，内部定义了RoundRobinRule，choose方法还是采用了RoundRobinRule的choose方法，所以它的选择策略和RoundRobinRule的选择策略一致，不赘述。

### BestAvailableRule

BestAvailableRule继承自ClientConfigEnabledRoundRobinRule，它在ClientConfigEnabledRoundRobinRule的基础上主要增加了根据loadBalancerStats中保存的服务实例的状态信息来过滤掉失效的服务实例的功能，然后顺便找出并发请求最小的服务实例来使用。然而loadBalancerStats有可能为null，如果loadBalancerStats为null，则BestAvailableRule将采用它的父类即ClientConfigEnabledRoundRobinRule的服务选取策略（线性轮询）。

### PredicateBasedRule

PredicateBasedRule是ClientConfigEnabledRoundRobinRule的一个子类，它先通过内部定义的一个过滤器过滤出一部分服务实例清单，然后再采用线性轮询的方式从过滤出来的结果中选取一个服务实例。

### ZoneAvoidanceRule

ZoneAvoidanceRule是PredicateBasedRule的一个实现类，只不过这里多一个过滤条件，ZoneAvoidanceRule中的过滤条件是以ZoneAvoidancePredicate为主过滤条件和以AvailabilityPredicate为次过滤条件组成的一个叫做CompositePredicate的组合过滤条件，过滤成功之后，继续采用线性轮询的方式从过滤结果中选择一个出来。
